package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.AppDesignPreset

// 1. EMERALD_GOLD (الملكي الزمردي والذهبي)
private val EmeraldDarkScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldSecondaryContainerDark,
    onSecondaryContainer = GoldOnSecondaryContainerDark,
    tertiary = CyanTertiaryDark,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5),
    onSurfaceVariant = Color(0xFFA7F3D0)
)

private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = GoldOnSecondaryLight,
    secondaryContainer = GoldSecondaryContainerLight,
    onSecondaryContainer = GoldOnSecondaryContainerLight,
    tertiary = CyanTertiaryLight,
    background = LightSurface,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = LightSurfaceVariant,
    onBackground = Color(0xFF064E3B),
    onSurface = Color(0xFF064E3B),
    onSurfaceVariant = Color(0xFF047857)
)

// 2. SAPPHIRE_FINTECH (البنكي السماوي الحديث - Neo-Bank Sapphire)
private val SapphireDarkScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF818CF8),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF2DD4BF),
    background = Color(0xFF090F1D),
    surface = Color(0xFF0D1526),
    surfaceVariant = Color(0xFF16223B),
    onBackground = Color(0xFFF0F9FF),
    onSurface = Color(0xFFF0F9FF),
    onSurfaceVariant = Color(0xFFBAE6FD)
)

private val SapphireLightScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFF0D9488),
    background = Color(0xFFF0F7FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2EEFA),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF0369A1)
)

// 3. AMETHYST_GLASS (الزجاجي البنفسجي والمرجاني - Cyber Amethyst)
private val AmethystDarkScheme = darkColorScheme(
    primary = Color(0xFFC084FC),
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFF581C87),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF4C0519),
    secondaryContainer = Color(0xFF881337),
    onSecondaryContainer = Color(0xFFFFE4E6),
    tertiary = Color(0xFF22D3EE),
    background = Color(0xFF110A1E),
    surface = Color(0xFF160E26),
    surfaceVariant = Color(0xFF24163D),
    onBackground = Color(0xFFFAF5FF),
    onSurface = Color(0xFFFAF5FF),
    onSurfaceVariant = Color(0xFFE9D5FF)
)

private val AmethystLightScheme = lightColorScheme(
    primary = Color(0xFF7E22CE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3E8FF),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = Color(0xFFE11D48),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF4C0519),
    tertiary = Color(0xFF0891B2),
    background = Color(0xFFFAF5FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3E8FF),
    onBackground = Color(0xFF2E1065),
    onSurface = Color(0xFF2E1065),
    onSurfaceVariant = Color(0xFF6B21A8)
)

// 4. MINIMAL_EXECUTIVE (التنفيذي الهندسي عالي التباين - Slate & Copper)
private val ExecutiveDarkScheme = darkColorScheme(
    primary = Color(0xFFF97316),
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFFE2E8F0),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF090A0F),
    surface = Color(0xFF11131A),
    surfaceVariant = Color(0xFF1C202B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val ExecutiveLightScheme = lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF431407),
    secondary = Color(0xFF1E293B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF0284C7),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF334155)
)

// 5. WARM_OASIS (الأبيض والبيج الهادئ - Warm Alabaster White & Soft Sand Beige)
private val OasisDarkScheme = darkColorScheme(
    primary = BeigePrimaryDark,
    onPrimary = BeigeOnPrimaryDark,
    primaryContainer = BeigePrimaryContainerDark,
    onPrimaryContainer = BeigeOnPrimaryContainerDark,
    secondary = Color(0xFF8FB38A),
    onSecondary = Color(0xFF1A2E17),
    secondaryContainer = Color(0xFF2B4228),
    onSecondaryContainer = Color(0xFFDDF0DA),
    tertiary = Color(0xFFE09F87),
    background = BeigeBackgroundDark,
    surface = BeigeSurfaceDark,
    surfaceVariant = BeigeSurfaceVariantDark,
    onBackground = Color(0xFFF7F2EB),
    onSurface = Color(0xFFF7F2EB),
    onSurfaceVariant = Color(0xFFD4C8BA),
    outline = Color(0xFF4A4036),
    outlineVariant = Color(0xFF383028)
)

private val OasisLightScheme = lightColorScheme(
    primary = BeigePrimaryLight,
    onPrimary = BeigeOnPrimaryLight,
    primaryContainer = BeigePrimaryContainerLight,
    onPrimaryContainer = BeigeOnPrimaryContainerLight,
    secondary = BeigeSecondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BeigeSecondaryContainerLight,
    onSecondaryContainer = BeigeOnSecondaryContainerLight,
    tertiary = BeigeTertiaryLight,
    background = BeigeBackgroundLight,
    surface = BeigeSurfaceLight,
    surfaceVariant = BeigeSurfaceVariantLight,
    onBackground = BeigeTextPrimaryLight,
    onSurface = BeigeTextPrimaryLight,
    onSurfaceVariant = BeigeTextSecondaryLight,
    outline = BeigeBorderLight,
    outlineVariant = Color(0xFFF0E9DD)
)

fun resolveDesignColorScheme(preset: AppDesignPreset, darkTheme: Boolean): ColorScheme =
    when (preset) {
        AppDesignPreset.EMERALD_GOLD -> if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
        AppDesignPreset.SAPPHIRE_FINTECH -> if (darkTheme) SapphireDarkScheme else SapphireLightScheme
        AppDesignPreset.AMETHYST_GLASS -> if (darkTheme) AmethystDarkScheme else AmethystLightScheme
        AppDesignPreset.MINIMAL_EXECUTIVE -> if (darkTheme) ExecutiveDarkScheme else ExecutiveLightScheme
        AppDesignPreset.WARM_OASIS -> if (darkTheme) OasisDarkScheme else OasisLightScheme
    }

fun resolveDesignShapes(preset: AppDesignPreset): Shapes =
    when (preset) {
        AppDesignPreset.MINIMAL_EXECUTIVE -> Shapes(
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp)
        )
        AppDesignPreset.AMETHYST_GLASS -> Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(30.dp)
        )
        AppDesignPreset.SAPPHIRE_FINTECH -> Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp)
        )
        else -> Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp)
        )
    }

@Composable
fun MizanFinanceTheme(
    darkTheme: Boolean = false,
    designPreset: AppDesignPreset = AppDesignPreset.WARM_OASIS,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveDesignColorScheme(designPreset, darkTheme)
    val shapes = resolveDesignShapes(designPreset)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}

