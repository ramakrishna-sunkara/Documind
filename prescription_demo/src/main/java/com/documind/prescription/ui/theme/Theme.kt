package com.documind.prescription.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF0066CC)
val PrimaryVariant = Color(0xFF004C99)
val Secondary = Color(0xFF10B981)
val SecondaryVariant = Color(0xFF059669)

val MedicalBlue = Color(0xFF0066CC)
val MedicalGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val OfflineBadge = Color(0xFF10B981)
val OnlineBadge = Color(0xFF3B82F6)

val GradientStart = Color(0xFF0066CC)
val GradientMiddle = Color(0xFF6B4EE6)
val GradientEnd = Color(0xFF10B981)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF001F33),
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002D1F),
    tertiary = Color(0xFF6B4EE6),
    onTertiary = Color.White,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF001F33),
    primaryContainer = Color(0xFF004C99),
    onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF002D1F),
    secondaryContainer = Color(0xFF059669),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF1F1147),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

@Composable
fun PrescriptionDemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
