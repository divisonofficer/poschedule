package com.jnkim.poschedule.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.ui.components.*
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiOnboardingScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    var validationState by remember { mutableStateOf<ValidationState>(ValidationState.Idle) }

    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    GlassBackground(accentColor = ModeNormal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.title_gemini_setup)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Description
                Text(
                    stringResource(R.string.desc_gemini_setup),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Key Input Section
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            stringResource(R.string.label_gemini_key),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        GlassTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                validationState = ValidationState.Idle
                            },
                            label = stringResource(R.string.hint_gemini_key),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Validation Status
                        AnimatedVisibility(
                            visible = validationState !is ValidationState.Idle,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (validationState) {
                                    is ValidationState.Testing -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            stringResource(R.string.status_testing),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    is ValidationState.Valid -> {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            stringResource(R.string.status_key_valid),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    is ValidationState.Invalid -> {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            stringResource(R.string.status_key_invalid),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassButton(
                                text = stringResource(R.string.action_test_key),
                                onClick = {
                                    if (apiKeyInput.isNotBlank()) {
                                        coroutineScope.launch {
                                            validationState = ValidationState.Testing
                                            val isValid = viewModel.validateAndSaveGeminiKey(apiKeyInput)
                                            validationState = if (isValid) {
                                                ValidationState.Valid
                                            } else {
                                                ValidationState.Invalid
                                            }
                                        }
                                    }
                                },
                                style = GlassButtonStyle.SECONDARY,
                                modifier = Modifier.weight(1f),
                                enabled = apiKeyInput.isNotBlank() && validationState !is ValidationState.Testing
                            )

                            GlassButton(
                                text = stringResource(R.string.action_save_key),
                                onClick = onBack,
                                style = GlassButtonStyle.PRIMARY,
                                modifier = Modifier.weight(1f),
                                enabled = validationState is ValidationState.Valid
                            )
                        }

                        // Security Notice
                        Text(
                            stringResource(R.string.warning_key_stored_locally),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Help Section (Progressive Disclosure)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.help_how_to_get_key),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { showHelp = !showHelp }) {
                                Icon(
                                    Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = showHelp,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    stringResource(R.string.help_gemini_intro),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                // Step-by-step guide
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        stringResource(R.string.help_step_1),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        stringResource(R.string.help_step_2),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        stringResource(R.string.help_step_3),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }

                                // Browser open button
                                GlassButton(
                                    text = stringResource(R.string.action_open_browser),
                                    onClick = {
                                        try {
                                            uriHandler.openUri("https://ai.google.dev/gemini-api/docs/api-key")
                                        } catch (e: Exception) {
                                            android.util.Log.e("GeminiOnboarding", "Failed to open browser: ${e.message}")
                                        }
                                    },
                                    style = GlassButtonStyle.PRIMARY,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    stringResource(R.string.help_note),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class ValidationState {
    object Idle : ValidationState()
    object Testing : ValidationState()
    object Valid : ValidationState()
    object Invalid : ValidationState()
}
