package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
import com.jnkim.poschedule.domain.model.RoutineType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@Composable
fun WeekOverviewGrid(
    weekStart: LocalDate,
    itemsByDay: Map<LocalDate, List<PlanItemEntity>>,
    onDaySelected: (LocalDate) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val days = (0..6).map { weekStart.plusDays(it.toLong()) }
        
        days.forEach { day ->
            val isToday = day == LocalDate.now()
            val dayItems = itemsByDay[day] ?: emptyList()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDaySelected(day) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Item Icons (emoji)
                Column(
                    modifier = Modifier
                        .height(120.dp)
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    dayItems.take(5).forEach { item ->
                        val emoji = getEmojiForItem(item)
                        val alpha = if (item.status == "DONE") 1f else 0.3f

                        Text(
                            text = emoji,
                            fontSize = 16.sp,
                            modifier = Modifier.alpha(alpha)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Maps a PlanItemEntity to its corresponding emoji icon.
 * Priority: LLM-generated iconEmoji > RoutineType default > keyword matching > generic icon
 */
private fun getEmojiForItem(item: PlanItemEntity): String {
    // 1. Prioritize LLM-generated emoji if available
    if (!item.iconEmoji.isNullOrBlank()) {
        return item.iconEmoji
    }

    // 2. Fall back to RoutineType defaults
    return when (item.type) {
        RoutineType.MEDS_AM -> "💊"
        RoutineType.MEDS_PM -> "💊"
        RoutineType.MEAL -> "🍽️"
        RoutineType.WIND_DOWN -> "😴"
        RoutineType.MOVEMENT -> "🏃"
        RoutineType.STUDY -> "📚"
        RoutineType.CHORE -> "🧹"
        null -> {
            // 3. For manual tasks without RoutineType, try keyword matching
            when {
                item.title.contains("공부", ignoreCase = true) || item.title.contains("학습", ignoreCase = true) -> "📚"
                item.title.contains("운동", ignoreCase = true) || item.title.contains("산책", ignoreCase = true) -> "🏃"
                item.title.contains("청소", ignoreCase = true) || item.title.contains("정리", ignoreCase = true) -> "🧹"
                item.title.contains("요리", ignoreCase = true) || item.title.contains("식사", ignoreCase = true) -> "🍽️"
                item.title.contains("회의", ignoreCase = true) || item.title.contains("미팅", ignoreCase = true) -> "💼"
                item.title.contains("약", ignoreCase = true) || item.title.contains("복용", ignoreCase = true) -> "💊"
                item.title.contains("수면", ignoreCase = true) || item.title.contains("잠", ignoreCase = true) -> "😴"
                else -> "📝"  // 4. Default task icon
            }
        }
    }
}
