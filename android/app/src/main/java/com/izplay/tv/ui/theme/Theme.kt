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

// Tokens oficiais do IZ Play Design System v1.0 (design-system/tokens/colors.json)
// cruzados com o CSS real do Web Player (--red #cc0000, --bg #0a0a0a, --txt #f0f0f0, --muted #888).
val IzRed = Color(0xFFCC0000)        // primary
val IzRedHover = Color(0xFFE60000)   // primaryHover
val IzRedSoft = Color(0xFFFF3333)    // primarySoft
val IzRedDark = Color(0xFF990000)
val IzRedDeep = Color(0xFF5A0000)
val IzYellow = Color(0xFFF5A623)     // destaque "AO VIVO/HOT" (âmbar)
val PanelBlack = Color(0xFF0A0A0A)   // background (Web --bg)
val PanelDark = Color(0xFF111111)    // surface
val PanelDarker = Color(0xFF0D0D0D)
val PanelElevated = Color(0xFF181818) // surface2
val SurfaceHover = Color(0xFF1B1B1B) // surfaceHover
val RowSelected = Color(0xFF2A0E0E)  // linha ativa (tint vermelho)
val TextPrimary = Color(0xFFF0F0F0)  // textPrimary (Web --txt)
val TextSecondary = Color(0xFFA8A8A8) // textSecondary
val Muted = Color(0xFF888888)        // muted (Web --muted)
val Divider = Color(0xFF2A2A2A)      // border

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
