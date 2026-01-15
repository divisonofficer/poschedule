package com.jnkim.poschedule.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.R
import com.jnkim.poschedule.ui.components.*
import com.jnkim.poschedule.ui.theme.ModeNormal
import com.jnkim.poschedule.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToGeminiSetup: () -> Unit,
    onLogout: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val isApiKeyPresent by viewModel.isApiKeyPresent.collectAsState()

    // Triple-tap detection state
    var tapCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    GlassBackground(accentColor = ModeNormal) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    tapCount++
                                    if (tapCount >= 3) {
                                        tapCount = 0
                                        onNavigateToDebug()
                                    } else {
                                        coroutineScope.launch {
                                            delay(1000)
                                            tapCount = 0
                                        }
                                    }
                                }
                            )
                        }
                    )
                },
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
                            GlassButton(
                                text = stringResource(R.string.action_logout),
                                onClick = onLogout,
                                style = GlassButtonStyle.SECONDARY
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        settings?.let { s ->
                            GlassTextField(
                                value = s.siteName,
                                onValueChange = { viewModel.updateSiteName(it) },
                                label = stringResource(R.string.label_site_name),
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

                // API Provider Section
                Text(stringResource(R.string.section_api_provider), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                // Experimental disclaimer (UI refinement Phase 4)
                Text(
                    text = stringResource(R.string.desc_api_experimental),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    settings?.let { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Provider Selection
                            Text(stringResource(R.string.label_api_provider), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.desc_api_provider), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                            GlassSegmentedControl(
                                options = listOf(
                                    stringResource(R.string.provider_postech),
                                    stringResource(R.string.provider_gemini)
                                ),
                                selectedIndex = if (s.apiProvider == "POSTECH") 0 else 1,
                                onSelectionChange = { index ->
                                    viewModel.updateApiProvider(if (index == 0) "POSTECH" else "GEMINI")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                style = SegmentedControlStyle.SOLID  // UI refinement Phase 4: important setting
                            )

                            // POSTECH Model Selection (only show if POSTECH selected)
                            if (s.apiProvider == "POSTECH") {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                Text(stringResource(R.string.label_postech_model), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.desc_postech_model), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                                GlassSegmentedControl(
                                    options = listOf(
                                        stringResource(R.string.model_gpt),
                                        stringResource(R.string.model_gemini),
                                        stringResource(R.string.model_claude)
                                    ),
                                    selectedIndex = when (s.postechModel) {
                                        "a1/gpt" -> 0
                                        "a2/gemini" -> 1
                                        "a3/claude" -> 2
                                        else -> 2
                                    },
                                    onSelectionChange = { index ->
                                        val model = when (index) {
                                            0 -> "a1/gpt"
                                            1 -> "a2/gemini"
                                            2 -> "a3/claude"
                                            else -> "a3/claude"
                                        }
                                        viewModel.updatePostechModel(model)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = SegmentedControlStyle.SOLID  // UI refinement Phase 4: important setting
                                )
                            }

                            // Gemini Configuration (only show if GEMINI selected)
                            if (s.apiProvider == "GEMINI") {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                // Gemini Model Selection
                                Text(stringResource(R.string.label_gemini_model), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.desc_gemini_model), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                                GlassSegmentedControl(
                                    options = listOf(
                                        stringResource(R.string.model_gemini_2_5_flash),
                                        stringResource(R.string.model_gemini_2_5_flash_lite),
                                        stringResource(R.string.model_gemini_2_5_pro)
                                    ),
                                    selectedIndex = when (s.geminiModel) {
                                        "gemini-2.5-flash" -> 0
                                        "gemini-2.5-flash-lite" -> 1
                                        "gemini-2.5-pro" -> 2
                                        else -> 0
                                    },
                                    onSelectionChange = { index ->
                                        val model = when (index) {
                                            0 -> "gemini-2.5-flash"
                                            1 -> "gemini-2.5-flash-lite"
                                            2 -> "gemini-2.5-pro"
                                            else -> "gemini-2.5-flash"
                                        }
                                        viewModel.updateGeminiModel(model)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = SegmentedControlStyle.SOLID  // UI refinement Phase 4: important setting
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                // Gemini API Key Configuration
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.label_gemini_key), style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            if (viewModel.isGeminiKeyConfigured()) {
                                                stringResource(R.string.status_gemini_configured)
                                            } else {
                                                stringResource(R.string.status_gemini_not_configured)
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (viewModel.isGeminiKeyConfigured()) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                    GlassButton(
                                        text = stringResource(R.string.action_configure_gemini),
                                        onClick = onNavigateToGeminiSetup,
                                        style = GlassButtonStyle.SECONDARY
                                    )
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
                            GlassToggle(checked = s.aiEnabled, onCheckedChange = { viewModel.toggleAiEnabled(it) })
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
                            GlassToggle(checked = s.visionConsent, onCheckedChange = { viewModel.toggleVisionConsent(it) })
                        }
                    }
                }

                // Display & Language Section
                Text(stringResource(R.string.section_display), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    settings?.let { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.label_language), style = MaterialTheme.typography.bodyLarge)
                            GlassSegmentedControl(
                                options = listOf(
                                    stringResource(R.string.lang_en),
                                    stringResource(R.string.lang_ko)
                                ),
                                selectedIndex = if (s.language == "en") 0 else 1,
                                onSelectionChange = { index ->
                                    viewModel.updateLanguage(if (index == 0) "en" else "ko")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Theme & Appearance Section
                Text(stringResource(R.string.section_theme), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    settings?.let { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Theme Mode Selection
                            Text(stringResource(R.string.label_theme_mode), style = MaterialTheme.typography.bodyLarge)
                            GlassSegmentedControl(
                                options = listOf(
                                    stringResource(R.string.theme_system),
                                    stringResource(R.string.theme_light),
                                    stringResource(R.string.theme_dark)
                                ),
                                selectedIndex = when (s.themeMode) {
                                    "SYSTEM" -> 0
                                    "LIGHT" -> 1
                                    "DARK" -> 2
                                    else -> -1
                                },
                                onSelectionChange = { index ->
                                    val mode = when (index) {
                                        0 -> "SYSTEM"
                                        1 -> "LIGHT"
                                        2 -> "DARK"
                                        else -> "SYSTEM"
                                    }
                                    viewModel.updateThemeMode(mode)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassChip(
                                text = stringResource(R.string.theme_time_adaptive),
                                isSelected = s.themeMode == "TIME_ADAPTIVE",
                                onClick = { viewModel.updateThemeMode("TIME_ADAPTIVE") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Weather Effects Toggle (only shown in TIME_ADAPTIVE mode)
                            if (s.themeMode == "TIME_ADAPTIVE") {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.label_weather_effects), style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            stringResource(R.string.desc_weather_effects),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    GlassToggle(
                                        checked = s.weatherEffectsEnabled,
                                        onCheckedChange = { viewModel.updateWeatherEffectsEnabled(it) }
                                    )
                                }

                                // Manual Weather Selection (only shown when weather effects enabled)
                                if (s.weatherEffectsEnabled) {
                                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                    Text(stringResource(R.string.label_current_weather), style = MaterialTheme.typography.bodyLarge)
                                    GlassSegmentedControl(
                                        options = listOf(
                                            stringResource(R.string.weather_clear),
                                            stringResource(R.string.weather_cloudy),
                                            stringResource(R.string.weather_rain),
                                            stringResource(R.string.weather_snow)
                                        ),
                                        selectedIndex = when (s.manualWeatherState) {
                                            "CLEAR" -> 0
                                            "CLOUDY" -> 1
                                            "RAIN" -> 2
                                            "SNOW" -> 3
                                            else -> 0
                                        },
                                        onSelectionChange = { index ->
                                            val state = when (index) {
                                                0 -> "CLEAR"
                                                1 -> "CLOUDY"
                                                2 -> "RAIN"
                                                3 -> "SNOW"
                                                else -> "CLEAR"
                                            }
                                            viewModel.updateManualWeatherState(state)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Notifications Section
                Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    settings?.let { s ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Status Companion Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_status_companion), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(R.string.settings_status_companion_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                GlassToggle(
                                    checked = s.statusCompanionEnabled,
                                    onCheckedChange = { viewModel.updateStatusCompanionEnabled(it) }
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                            // Lockscreen Details Toggle (only enabled if companion enabled)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (s.statusCompanionEnabled) 1.0f else 0.5f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_lockscreen_details), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        stringResource(R.string.settings_lockscreen_details_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                GlassToggle(
                                    checked = s.lockscreenDetailsEnabled,
                                    onCheckedChange = { viewModel.updateLockscreenDetailsEnabled(it) },
                                    enabled = s.statusCompanionEnabled
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
                        GlassButton(
                            text = "🐛 Simulate Missed Core (RECOVERY)",
                            onClick = { viewModel.simulateRecoveryScenario() },
                            modifier = Modifier.fillMaxWidth(),
                            style = GlassButtonStyle.SECONDARY
                        )
                        Text("Injects failure scenario to test adaptive mode transitions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
