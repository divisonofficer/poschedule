package com.jnkim.poschedule.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.domain.model.EmojiMapper
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.isReducedMotionEnabled

/**
 * Enhanced plan item card with emoji icon and state badges.
 *
 * Visual States:
 * - PENDING: Full opacity, emoji + title
 * - DONE: Glow effect, fade (alpha 0.5), slight shrink (scale 0.96)
 * - SKIPPED: Muted (alpha 0.5), light strikethrough
 * - SNOOZED: "💤" badge
 *
 * Design Features:
 * - Emoji icon left (from EmojiMapper fallback)
 * - State-aware styling with animations
 * - Completion animation (scale + alpha)
 * - Core items use SemiBold font
 *
 * Theme Integration:
 * - Background uses MaterialTheme.colorScheme.primary for done state (animates with phase)
 * - Text uses MaterialTheme.colorScheme.onSurface
 *
 * @param item Plan item entity
 * @param modifier Modifier for the card
 * @param iconEmoji Optional custom emoji (if null, uses EmojiMapper fallback)
 */
@Composable
fun PlanItemCard(
    item: PlanItemEntity,
    modifier: Modifier = Modifier,
    iconEmoji: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isReducedMotion = isReducedMotionEnabled()

    // Determine states from entity
    val isDone = item.status == "DONE"
    val isSkipped = item.status == "SKIPPED"
    val isSnoozed = item.snoozeUntil != null && item.snoozeUntil > System.currentTimeMillis()

    // Animate done/skipped state (or snap if reduced motion)
    val scale by animateFloatAsState(
        targetValue = if (isDone) 0.96f else 1.0f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "planScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isDone || isSkipped) 0.5f else 1.0f,
        animationSpec = if (isReducedMotion) snap() else tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "planAlpha"
    )

    // Get emoji icon (custom or fallback)
    val emoji = iconEmoji ?: EmojiMapper.getEmojiForPlan(
        title = item.title,
        planType = item.type?.name
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignTokens.Radius.md))
                .background(
                    if (isDone) {
                        // Subtle accent glow for completed items
                        colorScheme.primary.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    }
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Text(
                text = emoji,
                fontSize = 24.sp
            )

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isCore) FontWeight.SemiBold else FontWeight.Normal,
                color = colorScheme.onSurface,
                textDecoration = if (isSkipped) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f)
            )

            // State badges
            if (isSnoozed) {
                SnoozeBadge()
            }
        }
    }
}

/**
 * Small "zzz" badge for snoozed plan items.
 */
@Composable
private fun SnoozeBadge() {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(DesignTokens.Radius.xs))
            .background(colorScheme.secondary.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "💤",
            fontSize = 12.sp
        )
    }
}

/**
 * Simplified plan item for compact views (e.g., timeline).
 * Just emoji + title, no state badges.
 *
 * @param item Plan item entity
 * @param modifier Modifier for the item
 * @param iconEmoji Optional custom emoji
 */
@Composable
fun SimplePlanItem(
    item: PlanItemEntity,
    modifier: Modifier = Modifier,
    iconEmoji: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    val isDone = item.status == "DONE"
    val isSkipped = item.status == "SKIPPED"

    // Get emoji icon
    val emoji = iconEmoji ?: EmojiMapper.getEmojiForPlan(
        title = item.title,
        planType = item.type?.name
    )

    Row(
        modifier = modifier
            .alpha(if (isDone || isSkipped) 0.5f else 1.0f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp
        )

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (item.isCore) FontWeight.SemiBold else FontWeight.Normal,
            color = colorScheme.onSurface,
            textDecoration = if (isSkipped) TextDecoration.LineThrough else null,
            maxLines = 1
        )
    }
}
