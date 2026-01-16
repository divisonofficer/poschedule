package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.DesignTokens
import kotlinx.coroutines.delay

/**
 * Enhanced text field with Korean IME support, rotating placeholders, and character counter.
 *
 * Features:
 * - Korean IME composition tracking to avoid flicker
 * - Rotating placeholder examples every 3 seconds
 * - Inline autocomplete suggestion overlay
 * - Character counter (0/200 limit)
 * - Glass-morphism design integration
 * - Focus state visual feedback
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param onFocusChange Callback when focus state changes
 * @param onCompositionChange Callback when Korean IME composition state changes
 * @param inlineSuggestion Optional inline suggestion text to show as overlay
 * @param onAcceptInlineSuggestion Callback when inline suggestion is accepted (Tab key)
 * @param enabled Whether the text field is enabled
 * @param modifier Modifier for layout customization
 */
@Composable
fun EnhancedLLMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onCompositionChange: (Boolean) -> Unit,
    inlineSuggestion: String? = null,
    onAcceptInlineSuggestion: (() -> Unit)? = null,
    forceUpdateTrigger: Int = 0,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Track TextFieldValue to access composition state
    // DO NOT sync with external value during composition to preserve Korean IME state
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(value, selection = androidx.compose.ui.text.TextRange(value.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

    // Update internal state only when external value changes AND we're not composing
    LaunchedEffect(value) {
        if (textFieldValue.composition == null && textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, selection = androidx.compose.ui.text.TextRange(value.length))
        }
    }

    // Force update when trigger changes (ignore composition state)
    LaunchedEffect(forceUpdateTrigger) {
        if (forceUpdateTrigger > 0 && textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, selection = androidx.compose.ui.text.TextRange(value.length))
        }
    }

    // Rotating placeholder examples
    val placeholderExamples = remember {
        listOf(
            "내일 오후 3시에 과제 제출",
            "다음 주 월요일 회의 참석",
            "점심 먹고 운동하기",
            "저녁 7시에 친구 만나기"
        )
    }

    // Rotate placeholder every 3 seconds
    val currentPlaceholder by produceState(initialValue = placeholderExamples[0]) {
        var index = 0
        while (true) {
            delay(3000)
            index = (index + 1) % placeholderExamples.size
            this.value = placeholderExamples[index]
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = DesignTokens.Alpha.glassDefault
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Detect Korean IME composition state
                    val isComposing = newValue.composition != null
                    onCompositionChange(isComposing)

                    // Limit to 200 characters
                    if (newValue.text.length <= 200) {
                        textFieldValue = newValue
                        onValueChange(newValue.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        onFocusChange(focusState.isFocused)
                    },
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                decorationBox = { innerTextField ->
                    Box {
                        // Show placeholder when empty
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = currentPlaceholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }

                        // Show inline autocomplete suggestion
                        if (!textFieldValue.text.isEmpty() && inlineSuggestion != null) {
                            val currentText = textFieldValue.text
                            val suggestionLower = inlineSuggestion.lowercase()
                            val currentLower = currentText.lowercase()

                            // Only show if suggestion starts with current text
                            if (suggestionLower.startsWith(currentLower) && suggestionLower != currentLower) {
                                // Calculate the remaining part of the suggestion
                                val remainingSuggestion = inlineSuggestion.substring(currentText.length)

                                Row {
                                    // Invisible text to position the suggestion correctly
                                    Text(
                                        text = currentText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                    // Gray suggestion overlay
                                    Text(
                                        text = remainingSuggestion,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }

                        innerTextField()
                    }
                }
            )
        }

        // Character counter
        Text(
            text = "${textFieldValue.text.length}/200",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp, end = 4.dp)
        )
    }
}
