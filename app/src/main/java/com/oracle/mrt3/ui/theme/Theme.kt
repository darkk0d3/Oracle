package com.oracle.mrt3.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryGreen    = Color(0xFF00703C)
val SecondaryGreen  = Color(0xFF00A95C)
val BackgroundColor = Color(0xFFF2F2F2)
val SurfaceColor    = Color(0xFFFFFFFF)
val ErrorColor      = Color(0xFFEF4444)
val GoldAccent      = Color(0xFFFFD700)
val TextSecondary   = Color(0xFF64748B)
val DarkCard        = Color(0xFF16211C)

private val OracleLightColorScheme = lightColorScheme(
    primary            = PrimaryGreen,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFB7F0D1),
    onPrimaryContainer = Color(0xFF002112),
    secondary          = SecondaryGreen,
    onSecondary        = Color.White,
    background         = BackgroundColor,
    onBackground       = Color(0xFF1A1C19),
    surface            = SurfaceColor,
    onSurface          = Color(0xFF1A1C19),
    surfaceVariant     = Color(0xFFDDE5DB),
    onSurfaceVariant   = TextSecondary,
    error              = ErrorColor,
    onError            = Color.White,
)

@Composable
fun OracleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OracleLightColorScheme,
        content = content
    )
}
