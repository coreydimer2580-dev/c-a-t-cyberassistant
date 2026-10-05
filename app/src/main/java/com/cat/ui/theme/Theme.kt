package com.cat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** v1.16: user-picked neon accent. Default is the classic cyan. */
enum class NeonAccent(val id: String, val label: String, val color: Color) {
    CYAN("cyan", "Cyan", Color(0xFF00FFFF)),
    LIME("lime", "Lime", Color(0xFFB8FF3D)),
    MAGENTA("magenta", "Magenta", Color(0xFFFF4FF0)),
    ORANGE("orange", "Orange", Color(0xFFFF9A1F)),
    VIOLET("violet", "Violet", Color(0xFFA77BFF)),
    ICE("ice", "Ice", Color(0xFF8FE9FF));

    companion object {
        fun fromId(id: String?): NeonAccent = entries.firstOrNull { it.id == id } ?: CYAN
    }
}

object AccentState {
    var current by mutableStateOf(NeonAccent.CYAN)
}

/** Primary accent. Reads [AccentState] so the whole UI follows the picked colour. */
val NeonCyan: Color
    get() = AccentState.current.color
val NeonMagenta = Color(0xFFFF00EA)
val NeonLime = Color(0xFFD6FF00)
val Ink = Color(0xFF000000)
val Panel = Color(0xFF040509)
val PanelRaised = Color(0xFF0B0D12)
/** v1.16: deeper black for the Fold inner display. */
val FoldInk = Color(0xFF010102)
val FoldRail = Color(0xFF030408)
val Mist = Color(0xFFBFE8FF)
val Paper = Color(0xFFEAFBFF)

private fun catColors() = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Ink,
    primaryContainer = NeonCyan.copy(alpha = 0.22f),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A0033),
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonLime,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF1A3300),
    onTertiaryContainer = NeonLime,
    background = Ink,
    onBackground = Paper,
    surface = Panel,
    onSurface = Paper,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = Mist,
    outline = NeonCyan,
    error = NeonMagenta,
    onError = Ink
)

private val CATType = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 56.sp,
        letterSpacing = 1.sp,
        color = NeonCyan
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.4.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = Paper
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun CATTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = catColors(),
        typography = CATType,
        content = content
    )
}
