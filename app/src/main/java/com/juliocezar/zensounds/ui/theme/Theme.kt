package com.juliocezar.zensounds.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.EssencePreferences
import com.juliocezar.zensounds.data.ThemeMode

data class EssencePalette(
    val background: Color, val surface: Color, val text: Color, val secondaryText: Color,
    val accent: Color, val onAccent: Color, val verseBackground: Color, val onVerse: Color,
    val lilac: Color, val outline: Color, val track: Color, val foliage: Color
)
val EssenceLight = EssencePalette(
    Color(0xFFF5F1EB), Color(0xFFFFFDF9), Color(0xFF292622), Color(0xFF6E6864),
    Color(0xFFA3533C), Color.White, Color(0xFF98503B), Color(0xFFFFFAF3),
    Color(0xFFE5DEEB), Color(0xFFE5DDD5), Color(0xFFDED6E0), Color(0xFF656747)
)
val EssenceDark = EssencePalette(
    Color(0xFF181719), Color(0xFF242226), Color(0xFFF5EFE7), Color(0xFFBAB1AC),
    Color(0xFFE3AA8E), Color(0xFF2C1B15), Color(0xFF754231), Color(0xFFFFF6EB),
    Color(0xFF39323F), Color(0xFF3B3539), Color(0xFF49404B), Color(0xFFB8BA92)
)
val LocalEssencePalette = staticCompositionLocalOf { EssenceLight }
data class ThemeController(val mode: ThemeMode, val setMode: (ThemeMode) -> Unit)
val LocalThemeController = staticCompositionLocalOf { ThemeController(ThemeMode.SYSTEM) {} }
val EssenceFont = FontFamily(
    Font(R.font.essence_jakarta_regular, FontWeight.Normal),
    Font(R.font.essence_jakarta_medium, FontWeight.Medium),
    Font(R.font.essence_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.essence_jakarta_bold, FontWeight.Bold)
)
private val EssenceTypography = Typography(
    displaySmall = TextStyle(fontFamily = EssenceFont, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-1.1).sp),
    headlineMedium = TextStyle(fontFamily = EssenceFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = EssenceFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = EssenceFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = EssenceFont, fontSize = 18.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontFamily = EssenceFont, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = EssenceFont, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = EssenceFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = EssenceFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun ZenSoundsTheme(darkTheme: Boolean? = null, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context.applicationContext) { EssencePreferences(context.applicationContext) }
    DisposableEffect(preferences) { onDispose { preferences.close() } }
    val state by preferences.state.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val dark = darkTheme ?: when (state.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val p = if (dark) EssenceDark else EssenceLight
    // Paleta própria preserva a aparência aprovada, independente do papel de parede.
    val scheme = if (dark) darkColorScheme(
        primary=p.accent, onPrimary=p.onAccent, secondary=p.lilac, onSecondary=p.text,
        background=p.background, onBackground=p.text, surface=p.surface, onSurface=p.text,
        surfaceVariant=p.lilac, onSurfaceVariant=p.secondaryText, outline=p.outline
    ) else lightColorScheme(
        primary=p.accent, onPrimary=p.onAccent, secondary=p.lilac, onSecondary=p.text,
        background=p.background, onBackground=p.text, surface=p.surface, onSurface=p.text,
        surfaceVariant=p.lilac, onSurfaceVariant=p.secondaryText, outline=p.outline
    )
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalEssencePalette provides p, LocalThemeController provides ThemeController(state.themeMode, preferences::setTheme)) {
        MaterialTheme(colorScheme=scheme, typography=EssenceTypography, content=content)
    }
}
