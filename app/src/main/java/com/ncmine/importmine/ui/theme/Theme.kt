package com.ncmine.importmine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Esquema de cores Material 3 - completamente escuro com acentos neon
 */
private val NcDarkColorScheme = darkColorScheme(
    primary          = NcGreenNeon,
    onPrimary        = NcBlackDeep,
    primaryContainer = NcGreenNeonDark,
    onPrimaryContainer = NcGreenNeon,

    secondary        = NcGreenNeonDim,
    onSecondary      = NcBlackDeep,
    secondaryContainer = Color(0xFF003320),
    onSecondaryContainer = NcGreenNeonDim,

    tertiary         = NcInfo,
    onTertiary       = NcBlackDeep,

    background       = NcBlackDeep,
    onBackground     = NcTextPrimary,

    surface          = NcBlackCard,
    onSurface        = NcTextPrimary,
    surfaceVariant   = NcBlackSurface,
    onSurfaceVariant = NcTextSecondary,

    outline          = NcBlackBorder,
    outlineVariant   = Color(0xFF333333),

    error            = NcError,
    onError          = Color.White,
    errorContainer   = Color(0xFF4D0000),
    onErrorContainer = NcError,

    inverseSurface   = NcTextPrimary,
    inverseOnSurface = NcBlackDeep,
    inversePrimary   = NcGreenNeonDark,

    scrim            = Color(0xCC000000)
)

/**
 * Tema principal do NCMINE Addon Finder
 */
@Composable
fun NCMineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NcDarkColorScheme,
        typography = NcTypography,
        content = content
    )
}
