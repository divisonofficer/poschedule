package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthViewHeatmap(
    currentMonth: YearMonth,
    itemsByDay: Map<LocalDate, Int>, // Day to count of items
    onDaySelected: (LocalDate) -> Unit,
    accentColor: Color
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0=Sun, 1=Mon...

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Month Title
        Text(
            text = currentMonth.month.name.lowercase().capitalize() + " " + currentMonth.year,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            // Weekday Headers
            val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
            items(weekdays) { day ->
                Box(contentAlignment = Alignment.Center) {
                    Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }

            // Empty slots before first day
            items(firstDayOfWeek) { Spacer(Modifier) }

            // Days of the month
            items(daysInMonth) { dayIndex ->
                val day = firstDayOfMonth.plusDays(dayIndex.toLong())
                val count = itemsByDay[day] ?: 0
                val intensity = (count.toFloat() / 10f).coerceIn(0.1f, 1.0f)
                val isToday = day == LocalDate.now()

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (count > 0) accentColor.copy(alpha = intensity)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                        .clickable { onDaySelected(day) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.White)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}
