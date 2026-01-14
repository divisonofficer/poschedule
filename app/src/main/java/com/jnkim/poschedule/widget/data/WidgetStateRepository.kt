package com.jnkim.poschedule.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.widget.model.UrgencyLevel
import com.jnkim.poschedule.widget.model.WidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for widget state persistence using DataStore.
 *
 * Stores lightweight widget state data that survives app restarts.
 * Full PlanItemEntity is not stored - only essential display data is persisted.
 */
@Singleton
class WidgetStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_state")

    companion object {
        private val KEY_TASK_ID = stringPreferencesKey("task_id")
        private val KEY_TASK_TITLE = stringPreferencesKey("task_title")
        private val KEY_TIME_UNTIL_START = stringPreferencesKey("time_until_start")
        private val KEY_URGENCY_LEVEL = stringPreferencesKey("urgency_level")
        private val KEY_MODE = stringPreferencesKey("mode")
        private val KEY_LAST_UPDATED = longPreferencesKey("last_updated")
    }

    /**
     * Flow of current widget state.
     * Emits updates whenever state changes in DataStore.
     */
    val widgetStateFlow: Flow<WidgetState> = context.widgetDataStore.data.map { preferences ->
        WidgetState(
            task = null, // Full task entity not stored, fetched separately by worker
            timeUntilStart = preferences[KEY_TIME_UNTIL_START],
            urgencyLevel = preferences[KEY_URGENCY_LEVEL]?.let {
                UrgencyLevel.valueOf(it)
            } ?: UrgencyLevel.NORMAL,
            mode = preferences[KEY_MODE]?.let {
                Mode.valueOf(it)
            } ?: Mode.NORMAL,
            lastUpdated = preferences[KEY_LAST_UPDATED] ?: System.currentTimeMillis()
        )
    }

    /**
     * Update widget state in DataStore.
     * Called by WidgetUpdateWorker after computing new state.
     *
     * @param state New widget state to persist
     */
    suspend fun updateState(state: WidgetState) {
        context.widgetDataStore.edit { preferences ->
            // Store lightweight display data only
            preferences[KEY_TASK_TITLE] = state.task?.title ?: ""
            preferences[KEY_TASK_ID] = state.task?.id ?: ""
            preferences[KEY_TIME_UNTIL_START] = state.timeUntilStart ?: ""
            preferences[KEY_URGENCY_LEVEL] = state.urgencyLevel.name
            preferences[KEY_MODE] = state.mode.name
            preferences[KEY_LAST_UPDATED] = state.lastUpdated
        }
    }

    /**
     * Get current widget state synchronously from DataStore.
     * Used by widget when DataStore flow is not available.
     *
     * @return Current widget state or default state if none exists
     */
    suspend fun getCurrentState(): WidgetState {
        var state = WidgetState()
        context.widgetDataStore.data.collect { preferences ->
            state = WidgetState(
                task = null,
                timeUntilStart = preferences[KEY_TIME_UNTIL_START],
                urgencyLevel = preferences[KEY_URGENCY_LEVEL]?.let { level ->
                    UrgencyLevel.valueOf(level)
                } ?: UrgencyLevel.NORMAL,
                mode = preferences[KEY_MODE]?.let { mode ->
                    Mode.valueOf(mode)
                } ?: Mode.NORMAL,
                lastUpdated = preferences[KEY_LAST_UPDATED] ?: System.currentTimeMillis()
            )
        }
        return state
    }

    /**
     * Clear widget state (e.g., when all tasks are completed).
     */
    suspend fun clearState() {
        context.widgetDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
