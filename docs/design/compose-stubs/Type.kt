// app/src/main/java/com/coach/screentime/ui/theme/Type.kt
package com.coach.screentime.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.coach.screentime.R

val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

val Geist = FontFamily(
    Font(R.font.geist_regular,    FontWeight.Normal),
    Font(R.font.geist_medium,     FontWeight.Medium),
    Font(R.font.geist_semibold,   FontWeight.SemiBold),
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
)

// Single source of truth for our extended scale. The MaterialTheme `Typography`
// below maps these to Material's slots so anything that uses M3 defaults inherits
// our families automatically; project-internal screens should prefer these names.
object CoachType {
    val display = TextStyle(
        fontFamily = InstrumentSerif, fontStyle = FontStyle.Italic,
        fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-0.5).sp,
    )
    val headlineLg = TextStyle(fontFamily = InstrumentSerif, fontSize = 36.sp, lineHeight = 40.sp)
    val headlineMd = TextStyle(fontFamily = InstrumentSerif, fontSize = 26.sp, lineHeight = 30.sp)
    val headlineSm = TextStyle(fontFamily = InstrumentSerif, fontSize = 22.sp, lineHeight = 28.sp)
    val titleMd    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val titleSm    = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 20.sp)
    val bodyMd     = TextStyle(fontFamily = Geist, fontSize = 14.sp, lineHeight = 22.sp)
    val bodySm     = TextStyle(fontFamily = Geist, fontSize = 13.sp, lineHeight = 20.sp)
    val meta       = TextStyle(fontFamily = Geist, fontSize = 12.sp, lineHeight = 17.sp)
    val eyebrow    = TextStyle(
        fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
    )
    val mono       = TextStyle(fontFamily = GeistMono, fontSize = 13.sp, lineHeight = 18.sp)
}

val CoachTypography = Typography(
    displayLarge   = CoachType.display,
    headlineLarge  = CoachType.headlineLg,
    headlineMedium = CoachType.headlineMd,
    headlineSmall  = CoachType.headlineSm,
    titleLarge     = CoachType.titleMd,
    titleMedium    = CoachType.titleSm,
    bodyLarge      = CoachType.bodyMd,
    bodyMedium     = CoachType.bodyMd,
    bodySmall      = CoachType.bodySm,
    labelLarge     = CoachType.titleSm,
    labelMedium    = CoachType.meta,
    labelSmall     = CoachType.eyebrow,
)
