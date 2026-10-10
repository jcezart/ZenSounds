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
    val background: Color,
    val surface: Color,
    val text: Color,
    val secondaryText: Color,
    val accent: Color,
    val onAccent: Color,
    val verseBackground: Color,
    val onVerse: Color,
    val lilac: Color,
    val outline: Color,
    val track: Color,
    val proBackground: Color,
    val foliage: Color
)

val EssenceLight = EssencePalette(
    background = Color(0xFFF5F1EB),
    surface = Color(0xFFFFFDF9),
    text = Color(0xFF292622),
    secondaryText = Color(0xFF6E6864),
    accent = Color(0xFFA3533C),
    onAccent = Color.White,
    verseBackground = Color(0xFF98503B),
    onVerse = Color(0xFFFFFAF3),
    lilac = Color(0xFFE5DEEB),
    outline = Color(0xFFE5DDD5),
    track = Color(0xFFDED6E0),
    proBackground = Color(0xFFF0DED5),
    foliage = Color(0xFF656747)
)

val EssenceDark = EssencePalette(
    background = Color(0xFF181719),
    surface = Color(0xFF242226),
    text = Color(0xFFF5EFE7),
    secondaryText = Color(0xFFBAB1AC),
    accent = Color(0xFFE3AA8E),
    onAccent = Color(0xFF2C1B15),
    verseBackground = Color(0xFF754231),
    onVerse = Color(0xFFFFF6EB),
    lilac = Color(0xFF39323F),
    outline = Color(0xFF3B3539),
    track = Color(0xFF49404B),
    proBackground = Color(0xFF3B2A23),
    foliage = Color(0xFFB8BA92)
)

val LocalEssencePalette = staticCompositionLocalOf {
    EssenceLight
}

data class ThemeController(
    val mode: ThemeMode,
    val setMode: (ThemeMode) -> Unit
)

val LocalThemeController = staticCompositionLocalOf {
    ThemeController(ThemeMode.SYSTEM) {}
}

val EssenceFont = FontFamily(
    Font(R.font.essence_jakarta_regular, FontWeight.Normal),
    Font(R.font.essence_jakarta_medium, FontWeight.Medium),
    Font(R.font.essence_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.essence_jakarta_bold, FontWeight.Bold)
)

private val EssenceTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = EssenceFont,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EssenceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EssenceFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = EssenceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EssenceFont,
        fontSize = 18.sp,
        lineHeight = 30.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EssenceFont,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = EssenceFont,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EssenceFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = EssenceFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun ZenSoundsTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val preferences = remember(context.applicationContext) {
        EssencePreferences(context.applicationContext)
    }

    DisposableEffect(preferences) {
        onDispose {
            preferences.close()
        }
    }

    val state by preferences.state.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val dark = darkTheme ?: when (state.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val palette = if (dark) EssenceDark else EssenceLight

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.lilac,
            onSecondary = palette.text,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.lilac,
            onSurfaceVariant = palette.secondaryText,
            outline = palette.outline
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.lilac,
            onSecondary = palette.text,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.lilac,
            onSurfaceVariant = palette.secondaryText,
            outline = palette.outline
        )
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalEssencePalette provides palette,
        LocalThemeController provides ThemeController(
            mode = state.themeMode,
            setMode = preferences::setTheme
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EssenceTypography,
            content = content
        )
    }
}