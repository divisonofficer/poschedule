package com.jnkim.poschedule.widget.ui

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.LocalContext
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jnkim.poschedule.MainActivity
import com.jnkim.poschedule.domain.model.Mode
import com.jnkim.poschedule.notifications.NotificationConstants
import com.jnkim.poschedule.notifications.receiver.NotificationActionReceiver
import com.jnkim.poschedule.widget.model.UrgencyLevel
import com.jnkim.poschedule.widget.model.WidgetState
import com.jnkim.poschedule.workers.WidgetUpdateWorker

// Action parameter keys
val ACTION_KEY = ActionParameters.Key<String>("action")
val TASK_ID_KEY = ActionParameters.Key<String>("taskId")

/**
 * Medium widget content (4x2 cells).
 * Displays next pending task with action buttons.
 */
@Composable
fun MediumWidgetContent(
    state: WidgetState?,
    modifier: GlanceModifier = GlanceModifier
) {
    if (state == null || state.task == null) {
        // Empty state - no pending tasks
        EmptyWidgetContent(modifier = modifier)
    } else {
        // Active task display
        TaskWidgetContent(
            state = state,
            modifier = modifier
        )
    }
}

/**
 * Active task widget content.
 */
@Composable
fun TaskWidgetContent(
    state: WidgetState,
    modifier: GlanceModifier = GlanceModifier
) {
    val backgroundColor = when (state.mode) {
        Mode.RECOVERY -> Color(0xFFE8F5E9) // Light green
        else -> Color(0xFFF5F5F5) // Light gray
    }

    val urgencyColor = when (state.urgencyLevel) {
        UrgencyLevel.URGENT -> Color(0xFFEF5350) // Red
        UrgencyLevel.MODERATE -> Color(0xFFFFA726) // Orange
        UrgencyLevel.NORMAL -> Color(0xFF66BB6A) // Green
    }

    val taskId = state.task?.id ?: return

    Column(
        modifier = modifier
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        // Header row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "NEXT TASK",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.Gray)
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            // Mode badge
            if (state.mode == Mode.RECOVERY) {
                Text(
                    text = "RECOVERY",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF4CAF50))
                    ),
                    modifier = GlanceModifier
                        .background(Color.White)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Task title
        Text(
            text = state.task.title,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.Black)
            ),
            maxLines = 2
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Time info with urgency indicator
        Row(
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // Urgency dot
            Box(
                modifier = GlanceModifier
                    .size(8.dp)
                    .cornerRadius(4.dp)
                    .background(urgencyColor),
                content = {} // Empty content for colored dot
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = state.timeUntilStart ?: "Now",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = ColorProvider(urgencyColor)
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Action buttons row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Done button
            ActionButton(
                label = "✓ Done",
                backgroundColor = Color(0xFF4CAF50),
                action = actionRunCallback<WidgetActionCallback>(
                    parameters = actionParametersOf(
                        ACTION_KEY to NotificationConstants.ACTION_DONE,
                        TASK_ID_KEY to taskId
                    )
                )
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Snooze button
            ActionButton(
                label = "⏰ 15m",
                backgroundColor = Color(0xFFFFA726),
                action = actionRunCallback<WidgetActionCallback>(
                    parameters = actionParametersOf(
                        ACTION_KEY to NotificationConstants.ACTION_SNOOZE_15,
                        TASK_ID_KEY to taskId
                    )
                )
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Skip button
            ActionButton(
                label = "⏭ Skip",
                backgroundColor = Color(0xFF9E9E9E),
                action = actionRunCallback<WidgetActionCallback>(
                    parameters = actionParametersOf(
                        ACTION_KEY to NotificationConstants.ACTION_SKIP,
                        TASK_ID_KEY to taskId
                    )
                )
            )
        }
    }
}

/**
 * Empty state widget content.
 */
@Composable
fun EmptyWidgetContent(
    modifier: GlanceModifier = GlanceModifier
) {
    val context = LocalContext.current
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Column(
        modifier = modifier
            .background(Color(0xFFF5F5F5))
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(onClick = actionStartActivity(openAppIntent)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "🎉",
            style = TextStyle(fontSize = 32.sp)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "All clear today!",
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.Black)
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "No pending tasks",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color.Gray)
            )
        )
    }
}

/**
 * Action button component for widget.
 */
@Composable
fun ActionButton(
    label: String,
    backgroundColor: Color,
    action: Action
) {
    Text(
        text = label,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ColorProvider(Color.White)
        ),
        modifier = GlanceModifier
            .background(backgroundColor)
            .cornerRadius(8.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = action)
    )
}

/**
 * ActionCallback for widget button clicks.
 * Bridges widget actions to NotificationActionReceiver.
 */
class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val action = parameters[ACTION_KEY] ?: return
        val taskId = parameters[TASK_ID_KEY] ?: return

        // Create intent for NotificationActionReceiver
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationConstants.EXTRA_ROUTINE_ID, taskId)
            putExtra(NotificationConstants.EXTRA_WIDGET_UPDATE_TRIGGER, true)
        }

        // Send broadcast
        context.sendBroadcast(intent)
    }
}
