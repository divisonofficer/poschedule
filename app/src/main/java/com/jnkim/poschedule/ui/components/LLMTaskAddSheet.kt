package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R

/**
 * Bottom sheet for LLM-based task input.
 * Allows user to describe a task in natural language.
 */
@Composable
fun LLMTaskAddSheet(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.llm_task_input_placeholder)) },
                minLines = 3,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onSubmit(inputText) },
                    enabled = !isLoading && inputText.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.action_analyze))
                    }
                }
            }

            // Show witty loading indicator when processing
            if (isLoading) {
                Spacer(modifier = Modifier.height(32.dp))
                WittyLoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
