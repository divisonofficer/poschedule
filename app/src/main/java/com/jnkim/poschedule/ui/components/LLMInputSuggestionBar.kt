package com.jnkim.poschedule.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.domain.model.InputSuggestion
import com.jnkim.poschedule.ui.theme.DesignTokens

/**
 * Horizontal suggestion bar positioned above the keyboard.
 * Displays up to 3 context-aware suggestions as glass chips.
 *
 * Features:
 * - Slide-in/fade-in animation when suggestions appear
 * - Horizontal scrollable chip row
 * - Haptic feedback on chip tap
 * - Glass-morphism design with rounded top corners
 * - Automatically positioned above keyboard using imePadding
 *
 * @param suggestions List of suggestions to display (typically 3)
 * @param onSuggestionClick Callback when a suggestion chip is tapped
 * @param modifier Modifier for layout customization (should include imePadding)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LLMInputSuggestionBar(
    suggestions: List<InputSuggestion>,
    onSuggestionClick: (InputSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface.copy(
                alpha = DesignTokens.Layer.elevatedAlpha
            ),
            shadowElevation = 8.dp
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(suggestions, key = { it.text }) { suggestion ->
                    GlassChip(
                        text = "${suggestion.emoji ?: ""} ${suggestion.text}".trim(),
                        isSelected = false,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSuggestionClick(suggestion)
                        },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}
