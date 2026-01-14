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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnkim.poschedule.data.local.entity.PlanItemEntity
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

                // Density Orbs
                Column(
                    modifier = Modifier
                        .height(120.dp)
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayItems.take(5).forEach { item ->
                        val color = if (item.status == "DONE") accentColor else accentColor.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}
