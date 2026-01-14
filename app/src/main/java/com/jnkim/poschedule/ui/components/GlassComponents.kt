package com.jnkim.poschedule.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jnkim.poschedule.ui.theme.GlassDark
import com.jnkim.poschedule.ui.theme.GlassWhite

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) GlassDark else GlassWhite
    val borderColor = if (isDark) Color(0x1FFFFFFF) else Color(0x59FFFFFF)

    Surface(
        modifier = modifier,
        color = baseColor,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
fun GlassBackground(
    accentColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF0E1116) else Color(0xFFF5F7FA)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.15f),
                        bgColor,
                        bgColor
                    )
                )
            )
    ) {
        content()
    }
}
