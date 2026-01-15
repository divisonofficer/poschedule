# SHARE_INTENT_PLAN.md
# Poschedule — Share Intent → LLM Plan Extraction (Text / URL / Image / Email)
version: 1.0
target: Android (Kotlin, Jetpack Compose)
agent_target: Claude Code
scope: Add Android Share integration that converts shared content into normalized plans
status: implementation-ready spec (contracts + UX + safeguards)

---

## 0) Intent
Users often receive tasks/schedules via:
- SMS / messenger text
- email bodies
- web pages (URL, selected text)
- screenshots (event poster, timetable, reservation screen)

We will leverage Android **Share Intent** to let users “Share to Poschedule” and instantly:
1) ingest the shared payload
2) extract schedule intent
3) normalize into Plan JSON using the existing LLM normalizer
4) show a **Review Sheet** (confirm/edit)
5) save as PlanSeries/PlanInstance

This makes Poschedule a universal “inbox → plan” tool.

---

## 1) Non-negotiables
- Never auto-create plans without user confirmation.
- Minimize data sent to LLM (only what user shared + minimal context).
- Always show “What we received” (transparency).
- Provide graceful fallbacks (LLM down / offline / unsupported content).
- Avoid broad permissions (no reading SMS inbox; only shared content).

---

## 2) Supported Share Entry Types
### A) Text share
- From SMS, Kakao, Slack, etc.
- `ACTION_SEND` with `text/plain`

### B) URL share
- Browser share
- `text/plain` containing URL
- Optional: selected text share from web page

### C) Image share (screenshots/photos)
- `ACTION_SEND` with `image/*`
- `ACTION_SEND_MULTIPLE` with multiple images (optional)

### D) Email share
- Typically arrives as `text/plain` or `message/rfc822` depending on app
- Treat as text payload; attachments are out-of-scope for v1

---

## 3) UX: “Import Draft” Flow
### 3.1 Launch behavior
When app is opened via Share:
- Go directly to `ImportDraftScreen`
- Show a compact payload preview:
  - source app name (if available)
  - text snippet / URL / image thumbnail
  - privacy note: “Only what you shared is analyzed.”

### 3.2 Analyze & Review
- Tap “Analyze” (or auto-start with cancel option)
- Calm loading state
- On success: show `PlanReviewSheet` (reuse from LLM Task Add Mode)
- On ambiguity: show clarifications/options (reuse)
- Save/Edit/Discard

### 3.3 Multi-item support
If LLM returns multiple items:
- Show “N plans found”
- Let user toggle items on/off before saving

---

## 4) Android Implementation (Intents)
### 4.1 Manifest intent-filters
Create a dedicated entry Activity:
- `ShareReceiverActivity` (thin, routes into Compose)

Intent filters:

**Text**
- action: `android.intent.action.SEND`
- category: `android.intent.category.DEFAULT`
- mimeType: `text/plain`

**Image**
- action: `android.intent.action.SEND`
- mimeType: `image/*`
- (optional) action: `android.intent.action.SEND_MULTIPLE`

**Email compatibility**
- still covered by `text/plain`
- optionally add: `message/rfc822` (only if it helps; test across email apps)

### 4.2 Parsing payload
Handle:
- `Intent.EXTRA_TEXT` (String)
- `Intent.EXTRA_STREAM` (Uri) for images
- `ClipData` (some apps put multiple items here)

Extract metadata:
- `callingPackage` / `referrer`
- import timestamp

Represent as:
```kotlin
sealed class SharePayload {
  data class SharedText(val text: String, val source: String?) : SharePayload()
  data class SharedUrl(val url: String, val source: String?, val contextText: String?) : SharePayload()
  data class SharedImages(val uris: List<Uri>, val source: String?) : SharePayload()
}
```

---

## 5) URL Handling (no web scraping by default)
### v1 rule
Do NOT fetch full webpage content automatically.
Analyze only:
- the URL string
- any selected text user shared

Reason: privacy + paywalls + unpredictable HTML.

### v2 idea (out of scope)
If user taps “Extract from page” → fetch and summarize server-side.

---

## 6) Image Handling (Vision)
### 6.1 Pipeline
For images:
1) Read bytes from `content://` Uri via ContentResolver
2) Upload to Vision endpoint (reuse Tidy Snap infra or new endpoint)
3) Receive extracted text + structured fields (date/time/title/location keywords)
4) Feed extracted text into the same LLM normalizer as `raw_text`

### 6.2 Privacy
- Images are ephemeral by default.
- Store only if user saves and opts-in (optional “keep evidence”).
- Prefer not storing raw bytes; store only a reference or a tiny thumbnail.

---

## 7) LLM Normalization Contract (Reuse)
Reuse the schema in `LLM_BASED_TASK_ADD_PLAN.md`.
Wrapper request:
```json
{
  "source": "share_intent",
  "payloadType": "TEXT|URL|IMAGE",
  "rawText": "...",
  "url": "https://... (optional)",
  "visionExtract": "text (optional)",
  "now": "ISO",
  "timezone": "Asia/Seoul",
  "userDefaults": { "wake": "...", "bed": "...", "quietHours": "..." }
}
```

LLM should be allowed to return multiple items when needed.

---

## 8) Deterministic Safeguards (Mandatory)
After LLM JSON:
- Validate schema (kotlinx.serialization)
- Clamp offsets/durations
- Enforce quiet hours policy
- Conflict handling:
  - if conflicts with existing instances, suggest FLEX or alternate window
- Dedup check:
  - same title + same date/time within 5 min → show “Looks like duplicate” toggle

Always require confirmation UI.

---

## 9) Screens / Components
- `ShareReceiverActivity`: parse + route
- `ImportDraftScreen`: preview + analyze + cancel
- `PlanReviewSheet`: reuse (save/edit/discard)
- Optional: “Show source” accordion (original text or image thumbnail)

---

## 10) Error Handling UX
- Empty payload: “Nothing to import”
- LLM failure:
  - “Save as note” (creates basic task without time)
  - “Try again”
- Image upload failure:
  - offer manual text edit
  - open classic form

---

## 11) Testing (Acceptance)
- Text share: “내일 오후 3시 랩미팅” → meeting instance, clarifies if needed
- URL share: propose plan title, ask for time if missing
- Image share: poster screenshot → plan instance with date/time
- Email share: deadline sentence → HIGH importance task

Security:
- No SMS read permission.
- Only processes explicitly shared content.

---

## 12) Implementation Order (Claude Code)
1) ShareReceiverActivity + manifest filters
2) SharePayload parsing (EXTRA_TEXT/STREAM/ClipData)
3) ImportDraftScreen route + preview UI
4) URL detector + request builder
5) Image ingest pipeline (uri → bytes → upload → extract)
6) Hook to LLM normalizer + PlanReviewSheet
7) Post-processing + DB save mapping (Series/Instance)
8) Tests: parser, URL detect, dedup

---

## 13) Guardrails
- No automatic webpage scraping in v1.
- Do not persist raw shared payload unless user saves.
- No scary permissions prompts.
- Never silently add plans.

END OF FILE
