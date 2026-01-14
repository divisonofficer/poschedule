package com.jnkim.poschedule.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val SITE_NAME = stringPreferencesKey("site_name")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val VISION_CONSENT = booleanPreferencesKey("vision_consent")
        val DAILY_BUDGET = intPreferencesKey("daily_budget")
        val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            siteName = preferences[PreferencesKeys.SITE_NAME] ?: "robi-gpt-dev",
            aiEnabled = preferences[PreferencesKeys.AI_ENABLED] ?: false,
            visionConsent = preferences[PreferencesKeys.VISION_CONSENT] ?: false,
            dailyBudget = preferences[PreferencesKeys.DAILY_BUDGET] ?: 7,
            quietHoursStart = preferences[PreferencesKeys.QUIET_HOURS_START] ?: "23:00",
            quietHoursEnd = preferences[PreferencesKeys.QUIET_HOURS_END] ?: "07:00",
            language = preferences[PreferencesKeys.LANGUAGE] ?: "en"
        )
    }

    suspend fun updateAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AI_ENABLED] = enabled }
    }

    suspend fun updateVisionConsent(consent: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VISION_CONSENT] = consent }
    }

    suspend fun updateDailyBudget(budget: Int) {
        context.dataStore.edit { it[PreferencesKeys.DAILY_BUDGET] = budget }
    }

    suspend fun updateSiteName(name: String) {
        context.dataStore.edit { it[PreferencesKeys.SITE_NAME] = name }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[PreferencesKeys.LANGUAGE] = language }
    }
}

data class UserSettings(
    val siteName: String,
    val aiEnabled: Boolean,
    val visionConsent: Boolean,
    val dailyBudget: Int,
    val quietHoursStart: String,
    val quietHoursEnd: String,
    val language: String
)
