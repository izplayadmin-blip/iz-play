package com.izplay.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta inspirada na referência IZ Play
val IzRed = Color(0xFFD81F26)
val IzRedDark = Color(0xFF8E1419)
val IzRedDeep = Color(0xFF5A0C10)
val IzYellow = Color(0xFFFFB000)
val PanelBlack = Color(0xFF0A0A0C)
val PanelDark = Color(0xFF121215)
val PanelDarker = Color(0xFF0E0E11)
val PanelElevated = Color(0xFF17171C)
val RowSelected = Color(0xFF2A1416)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9A9AA2)
val Divider = Color(0xFF1E1E22)

private val IzColors = darkColorScheme(
    primary = IzRed,
    onPrimary = Color.White,
    background = PanelBlack,
    onBackground = TextPrimary,
    surface = PanelDark,
    onSurface = TextPrimary,
    secondary = IzRedDark,
)

private val IzTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun IZPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IzColors,
        typography = IzTypography,
        content = content
    )
}
