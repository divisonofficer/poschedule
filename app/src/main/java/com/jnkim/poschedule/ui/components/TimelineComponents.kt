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
import com.jnkim.poschedule.ui.theme.innerGlow

@Composable
fun TimelineNode(
    time: String,
    isPast: Boolean,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .alpha(if (isPast) 0.5f else 1.0f),
        verticalAlignment = Alignment.Top
    ) {
        // Time Label
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Vertical Line and Orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight()
        ) {
            // Timeline orb with inner glow for non-past items
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isPast) accentColor.copy(alpha = 0.4f) else accentColor)
                    .then(
                        // Add inner glow only for non-past items
                        if (!isPast) Modifier.innerGlow(glowRadius = 6.dp) else Modifier
                    )
            )
            // Vertical connecting line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(accentColor.copy(alpha = 0.2f))
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Glass Chip
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
fun TimelineLayout(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        content()
    }
}
