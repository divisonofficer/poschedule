package com.jnkim.poschedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import com.jnkim.poschedule.ui.theme.specularHighlight

/**
 * Glass-styled text field with floating label and focus glow.
 *
 * Design Features:
 * - Floating label animation
 * - Focus ring with accent glow
 * - Layer 3 surface when focused
 * - Helper text support
 *
 * Theme Integration:
 * - Background uses MaterialTheme.colorScheme.surface (Layer 3 when focused)
 * - Focus ring uses MaterialTheme.colorScheme.primary (animates with day phase)
 * - Text colors adapt to onSurface
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param label Floating label text
 * @param modifier Modifier for the text field
 * @param helperText Optional helper text below field
 * @param isError Whether field is in error state
 * @param singleLine Whether field is single line
 * @param keyboardOptions Keyboard options
 * @param keyboardActions Keyboard actions
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colorScheme = MaterialTheme.colorScheme
    var isFocused by remember { mutableStateOf(false) }

    // Animate focus state
    val focusAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "focusAlpha"
    )

    // Background color (Layer 3 when focused)
    val backgroundColor = colorScheme.surface.copy(
        alpha = if (isFocused) DesignTokens.Layer.focusedAlpha else DesignTokens.Alpha.glassDefault
    )

    // Border color (accent when focused, outline otherwise)
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colorScheme.error
            isFocused -> colorScheme.primary.copy(alpha = 0.6f)
            else -> colorScheme.outline.copy(alpha = DesignTokens.Alpha.borderDefault)
        },
        animationSpec = tween(durationMillis = DesignTokens.Animation.normalMs),
        label = "borderColor"
    )

    // Glow color for focus ring
    val glowColor = colorScheme.primary.copy(alpha = 0.3f * focusAlpha)

    // Label should float up when focused or has text
    val shouldFloatLabel = isFocused || value.isNotEmpty()

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignTokens.Layer.focusedRadius))
                .background(backgroundColor)
                .drawBehind {
                    // Focus glow (outer shadow)
                    if (focusAlpha > 0) {
                        drawRect(
                            color = glowColor,
                            size = size
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(DesignTokens.Layer.focusedRadius)
                )
                .specularHighlight(
                    cornerRadius = DesignTokens.Layer.focusedRadius,
                    intensity = if (isFocused) 1.0f else 0.6f
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                // Floating label
                if (shouldFloatLabel) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) {
                            colorScheme.primary
                        } else if (isError) {
                            colorScheme.error
                        } else {
                            colorScheme.onSurface.copy(alpha = DesignTokens.Alpha.muted)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty() && !shouldFloatLabel) {
                            // Show placeholder when empty and label not floating
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface.copy(alpha = DesignTokens.Alpha.muted)
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Helper text
        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) {
                    colorScheme.error
                } else {
                    colorScheme.onSurface.copy(alpha = DesignTokens.Alpha.muted)
                },
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
