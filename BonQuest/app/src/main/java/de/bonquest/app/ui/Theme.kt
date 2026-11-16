package de.bonquest.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GameColors {
    val Bg = Color(0xFF140B2E)
    val BgDeep = Color(0xFF0C0620)
    val Card = Color(0xFF23154A)
    val CardHi = Color(0xFF2F1E5F)
    val Border = Color(0xFF5B3FC4)
    val Purple = Color(0xFF8A5CFF)
    val Gold = Color(0xFFFFC83D)
    val GoldDark = Color(0xFFB8860B)
    val Xp = Color(0xFF3DDC84)
    val XpDark = Color(0xFF1E8C50)
    val Hp = Color(0xFFFF5370)
    val HpDark = Color(0xFFA3203A)
    val Mana = Color(0xFF4FC3F7)
    val ManaDark = Color(0xFF1F7DA8)
    val Orange = Color(0xFFFF9F43)
    val Text = Color(0xFFF3EEFF)
    val TextDim = Color(0xFFB3A6D9)
}

private val GameTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
)

@Composable
fun BonQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = GameColors.Purple,
            secondary = GameColors.Gold,
            tertiary = GameColors.Xp,
            background = GameColors.Bg,
            surface = GameColors.Card,
            surfaceVariant = GameColors.CardHi,
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = GameColors.Text,
            onSurface = GameColors.Text,
            onSurfaceVariant = GameColors.TextDim,
            outline = GameColors.Border,
            error = GameColors.Hp,
        ),
        typography = GameTypography,
        content = content,
    )
}
