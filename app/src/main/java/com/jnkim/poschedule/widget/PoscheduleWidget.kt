package com.jnkim.poschedule.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.fillMaxSize
import com.jnkim.poschedule.widget.data.WidgetStateRepository
import com.jnkim.poschedule.widget.ui.MediumWidgetContent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import javax.inject.Inject

/**
 * Poschedule home screen widget built with Jetpack Glance.
 *
 * Displays the next pending task with Done, Snooze, and Skip action buttons.
 * Updates automatically at window starts and after user actions.
 */
class PoscheduleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Get widget state repository via Hilt
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            PoscheduleWidgetEntryPoint::class.java
        )
        val widgetStateRepository = entryPoint.widgetStateRepository()

        provideContent {
            // Fetch current state
            val state by widgetStateRepository.widgetStateFlow.collectAsState(initial = null)

            // Render widget UI
            MediumWidgetContent(
                state = state,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

/**
 * GlanceAppWidgetReceiver for Poschedule widget.
 * Handles system widget lifecycle events (add, remove, update).
 */
@AndroidEntryPoint
class PoscheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PoscheduleWidget()

    @Inject
    lateinit var widgetStateRepository: WidgetStateRepository

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        // First widget instance added - schedule initial update
        // TODO: Trigger WidgetUpdateWorker
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        // Last widget instance removed - clean up if needed
    }
}
