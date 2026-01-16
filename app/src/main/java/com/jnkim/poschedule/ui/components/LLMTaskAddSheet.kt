package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.domain.ai.InputSuggestionEngine
import com.jnkim.poschedule.domain.model.InputSuggestion

/**
 * Bottom sheet for LLM-based task input with enhanced UX.
 * Features:
 * - Context-aware suggestions positioned above keyboard
 * - Korean IME composition tracking
 * - Rotating placeholder examples
 * - Character counter
 */
@Composable
fun LLMTaskAddSheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var isComposing by remember { mutableStateOf(false) }
    var lastSpaceTime by remember { mutableStateOf(0L) }
    var forceUpdateTrigger by remember { mutableStateOf(0) }

    // Get keyboard (IME) height for dynamic padding
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardVisiblePadding = 0.dp
    val suggestionEngine = remember { InputSuggestionEngine() }

    // Separate state for suggestions to avoid affecting TextField
    var suggestions by remember { mutableStateOf<List<InputSuggestion>>(emptyList()) }

    // Inline suggestion (first suggestion that starts with current input)
    val inlineSuggestion = remember(suggestions, inputText) {
        if (inputText.isNotBlank() && suggestions.isNotEmpty()) {
            val firstSuggestion = suggestions[0].insertText
            if (firstSuggestion.lowercase().startsWith(inputText.trim().lowercase())) {
                firstSuggestion
            } else null
        } else null
    }

    // Update suggestions independently (read-only from inputText)
    LaunchedEffect(inputText, isFocused, isLoading) {
        if (isFocused && !isLoading) {
            suggestions = suggestionEngine.generateSuggestions(
                currentText = inputText,
                locale = "ko"
            )
        } else {
            suggestions = emptyList()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.llm_task_input_title),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced text field with Korean IME support
            EnhancedLLMTextField(
                value = inputText,
                onValueChange = { newText ->
                    // Detect double space for auto-completion
                    if (newText.endsWith("  ") && inlineSuggestion != null) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastSpaceTime < 500) { // Within 500ms
                            // Apply inline suggestion and remove double space
                            inputText = inlineSuggestion
                            forceUpdateTrigger++
                            lastSpaceTime = 0L
                        } else {
                            inputText = newText
                            lastSpaceTime = currentTime
                        }
                    } else {
                        inputText = newText
                    }
                },
                onFocusChange = { focused -> isFocused = focused },
                onCompositionChange = { composing -> isComposing = composing },
                inlineSuggestion = inlineSuggestion,
                forceUpdateTrigger = forceUpdateTrigger,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    style = GlassButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                GlassButton(
                    text = stringResource(R.string.action_analyze),
                    onClick = { onSubmit(inputText) },
                    style = GlassButtonStyle.PRIMARY,
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotBlank() && !isLoading
                )
            }

            // Show witty loading indicator when processing
            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                WittyLoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Suggestion bar positioned above keyboard with dynamic padding
        LLMInputSuggestionBar(
            suggestions = suggestions,
            onSuggestionClick = { suggestion ->
                // Smart suggestion insertion: replace if overlap, append if different
                val currentTrimmed = inputText.trim()
                val suggestionText = suggestion.insertText

                inputText = when {
                    // Empty input: just insert suggestion
                    currentTrimmed.isEmpty() -> suggestionText

                    // Suggestion starts with current input: replace with suggestion
                    suggestionText.lowercase().startsWith(currentTrimmed.lowercase()) -> {
                        suggestionText
                    }

                    // Check if last word matches beginning of suggestion
                    else -> {
                        val words = currentTrimmed.split(" ")
                        val lastWord = words.lastOrNull() ?: ""

                        if (lastWord.isNotEmpty() && suggestionText.lowercase().startsWith(lastWord.lowercase())) {
                            // Replace last word with suggestion
                            val beforeLastWord = words.dropLast(1).joinToString(" ")
                            if (beforeLastWord.isEmpty()) {
                                suggestionText
                            } else {
                                "$beforeLastWord $suggestionText"
                            }
                        } else {
                            // No overlap: append with space
                            "$currentTrimmed $suggestionText"
                        }
                    }
                }

                // Force immediate update (ignore Korean IME composition)
                forceUpdateTrigger++
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = keyboardVisiblePadding)
        )
    }
}
