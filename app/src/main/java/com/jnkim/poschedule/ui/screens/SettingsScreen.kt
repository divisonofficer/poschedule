package com.jnkim.poschedule.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.ui.components.GlassBackground
import com.jnkim.poschedule.ui.components.GlassCard
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isApiKeyPresent by viewModel.isApiKeyPresent.collectAsState()

    GlassBackground(accentColor = ModeNormal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
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
                // Account Section
                Text(stringResource(R.string.section_account), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.label_sso_status), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.label_active_session), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                                Text(stringResource(R.string.action_logout))
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        settings?.let { s ->
                            OutlinedTextField(
                                value = s.siteName,
                                onValueChange = { viewModel.updateSiteName(it) },
                                label = { Text(stringResource(R.string.label_site_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.label_api_key), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (isApiKeyPresent) stringResource(R.string.status_key_configured) else stringResource(R.string.status_key_missing),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isApiKeyPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                                IconButton(onClick = { viewModel.fetchApiKey() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_fetch_key))
                                }
                            }
                        }
                    }
                }

                // AI Section
                Text(stringResource(R.string.section_ai), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    settings?.let { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_llm_copy), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.desc_llm_copy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Switch(checked = s.aiEnabled, onCheckedChange = { viewModel.toggleAiEnabled(it) })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_vision_consent), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.desc_vision_consent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Switch(checked = s.visionConsent, onCheckedChange = { viewModel.toggleVisionConsent(it) })
                        }
                    }
                }

                // Display & Language Section
                Text(stringResource(R.string.section_display), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    settings?.let { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.label_language), style = MaterialTheme.typography.bodyLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = s.language == "en",
                                    onClick = { viewModel.updateLanguage("en") },
                                    label = { Text(stringResource(R.string.lang_en)) }
                                )
                                FilterChip(
                                    selected = s.language == "ko",
                                    onClick = { viewModel.updateLanguage("ko") },
                                    label = { Text(stringResource(R.string.lang_ko)) }
                                )
                            }
                        }
                    }
                }

                // Developer / Simulation Section (New for Milestone 2.4)
                Text("Developer / Experiments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Life OS Replay Harness", style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = { viewModel.simulateRecoveryScenario() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate Missed Core (RECOVERY)")
                        }
                        Text("Injects failure scenario to test adaptive mode transitions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
