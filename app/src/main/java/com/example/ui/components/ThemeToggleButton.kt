package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoonIndigo
import com.example.ui.theme.SunGold

@Composable
fun ThemeToggleButton(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 360f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "theme_rotation"
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "theme_scale"
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (isDark) Color(0x33F59E0B) else Color(0x1F6366F1)
            )
            .border(
                width = 1.dp,
                color = if (isDark) SunGold.copy(alpha = 0.5f) else MoonIndigo.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onToggle)
            .scale(scale)
            .testTag("theme_toggle_button"),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = isDark,
            label = "theme_icon_crossfade",
            modifier = Modifier.rotate(rotation)
        ) { dark ->
            if (dark) {
                Icon(
                    imageVector = Icons.Default.LightMode,
                    contentDescription = "Ganti ke Mode Terang",
                    tint = SunGold,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.DarkMode,
                    contentDescription = "Ganti ke Mode Gelap",
                    tint = MoonIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
