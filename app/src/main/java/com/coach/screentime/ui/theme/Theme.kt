package com.coach.screentime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary            = AccentTerracotta,
    onPrimary          = Card,
    primaryContainer   = AlertSoft,
    onPrimaryContainer = Accent2,
    secondary          = SageGreen,
    onSecondary        = Card,
    tertiary           = WarnGold,
    background         = Paper,
    onBackground       = Ink,
    surface            = Card,
    onSurface          = Ink,
    surfaceVariant     = Card2,
    onSurfaceVariant   = InkMute,
    outline            = Rule,
    outlineVariant     = RuleSoft,
    error              = Alert,
    onError            = Card,
    errorContainer     = AlertSoft,
    onErrorContainer   = Alert,
)

private val DarkScheme = darkColorScheme(
    primary            = AccentTerracotta,
    onPrimary          = PaperOnDeep,
    secondary          = SageGreen,
    onSecondary        = PaperOnDeep,
    tertiary           = WarnGold,
    background         = InkDeep,
    onBackground       = PaperOnDeep,
    surface            = InkDeep2,
    onSurface          = PaperOnDeep,
    surfaceVariant     = InkDeep3,
    onSurfaceVariant   = PaperOnDeepMute,
    outline            = InkDeep3,
    outlineVariant     = InkDeep3,
    error              = Alert,
)

@Composable
fun ScreenTimeCoachTheme(
    darkTheme: Boolean = false,   // design is paper-light only; ignore system dark mode
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography  = CoachTypography,
        content     = content,
    )
}
