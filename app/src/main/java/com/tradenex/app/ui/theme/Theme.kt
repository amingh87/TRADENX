package com.tradenex.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.tradenex.app.data.AppState

/** How the globe is drawn: the original ocean globe or the gold dotted one. */
enum class GlobeStyle { OCEAN, GOLD }

/** Themes shown in Settings. [DEFAULT] is the look the project shipped with; [GOLD] is the luxe gold look. */
enum class AppTheme(val label: String, val tagline: String) {
    DEFAULT("Default", "کلاسیک و شیشه‌ای"),
    GOLD("GOLD", "طلایی، درخشان و لوکس");

    companion object {
        fun of(index: Int): AppTheme = entries.getOrElse(index) { DEFAULT }
    }
}

/** Every colour and style switch a theme controls. Screens read it through [Tx]. */
data class Palette(
    val bg: Color,
    val gold: Color,
    val goldHi: Color,
    val goldDeep: Color,
    val goldPale: Color,
    val green: Color,
    val greenDeep: Color,
    val red: Color,
    val redDeep: Color,
    val blue: Color,
    val purple: Color,
    val text: Color,
    val muted: Color,
    val ink: Color,
    val surface: Color,
    val cardA: Color,
    val cardB: Color,
    val navA: Color,
    val navB: Color,
    val sessSydney: Color,
    val sessTokyo: Color,
    val sessLondon: Color,
    val sessNewYork: Color,
    /** Luxe cards: gradient panels, glowing borders, shimmer sweep, gold FAB. */
    val luxe: Boolean,
    val globe: GlobeStyle,
)

object Palettes {
    val Default = Palette(
        bg = Color(0xFF07070A),
        gold = Color(0xFFF7CF6B),
        goldHi = Color(0xFFFFD978),
        goldDeep = Color(0xFFC98E1E),
        goldPale = Color(0xFFFFE9A8),
        green = Color(0xFF2BE8A8),
        greenDeep = Color(0xFF0C7A56),
        red = Color(0xFFFF6B7A),
        redDeep = Color(0xFFB9283A),
        blue = Color(0xFF4DA3FF),
        purple = Color(0xFFB58CFF),
        text = Color(0xFFF4EFE3),
        muted = Color(0xFFB4AE9F),
        ink = Color(0xFF1A1204),
        surface = Color(0xFF14141A),
        cardA = Color(0x17FFFFFF),
        cardB = Color(0x05FFFFFF),
        navA = Color(0xEB1E1A0E),
        navB = Color(0xF50A0A0C),
        sessSydney = Color(0xFF4DA3FF),
        sessTokyo = Color(0xFFB58CFF),
        sessLondon = Color(0xFFF7CF6B),
        sessNewYork = Color(0xFFFF6B7A),
        luxe = false,
        globe = GlobeStyle.OCEAN,
    )

    val Gold = Palette(
        bg = Color(0xFF070608),
        gold = Color(0xFFF2B53D),
        goldHi = Color(0xFFFFD884),
        goldDeep = Color(0xFFB9780F),
        goldPale = Color(0xFFFFE7A8),
        green = Color(0xFF1FD673),
        greenDeep = Color(0xFF128A4A),
        red = Color(0xFFFF3D4F),
        redDeep = Color(0xFFA3202D),
        blue = Color(0xFF2F8CFF),
        purple = Color(0xFFB36BFF),
        text = Color(0xFFF7EDD8),
        muted = Color(0xFFAB9E82),
        ink = Color(0xFF1A1004),
        surface = Color(0xFF17110A),
        cardA = Color(0xFF1A130B),
        cardB = Color(0xFF0D0A07),
        navA = Color(0xF51A130B),
        navB = Color(0xFA090705),
        sessSydney = Color(0xFF2F8CFF),
        sessTokyo = Color(0xFF1FD673),
        sessLondon = Color(0xFFFFB31F),
        sessNewYork = Color(0xFFFF3D4F),
        luxe = true,
        globe = GlobeStyle.GOLD,
    )

    fun of(theme: Int): Palette = if (AppTheme.of(theme) == AppTheme.GOLD) Gold else Default

    /** Accent 0 keeps the theme's gold; 1 = emerald, 2 = violet replace the four gold shades. */
    fun resolve(theme: Int, accent: Int): Palette {
        val p = of(theme)
        return when (accent) {
            1 -> p.copy(gold = Color(0xFF2BE8A8), goldHi = Color(0xFF7CF5CF), goldDeep = Color(0xFF0C9A70), goldPale = Color(0xFFC9FFEE))
            2 -> p.copy(gold = Color(0xFFB58CFF), goldHi = Color(0xFFD3B8FF), goldDeep = Color(0xFF6E3FD0), goldPale = Color(0xFFEBDDFF))
            else -> p
        }
    }
}

private fun mix(a: Palette, b: Palette, t: Float): Palette {
    fun c(x: Color, y: Color) = lerp(x, y, t)
    return Palette(
        bg = c(a.bg, b.bg), gold = c(a.gold, b.gold), goldHi = c(a.goldHi, b.goldHi),
        goldDeep = c(a.goldDeep, b.goldDeep), goldPale = c(a.goldPale, b.goldPale),
        green = c(a.green, b.green), greenDeep = c(a.greenDeep, b.greenDeep),
        red = c(a.red, b.red), redDeep = c(a.redDeep, b.redDeep),
        blue = c(a.blue, b.blue), purple = c(a.purple, b.purple),
        text = c(a.text, b.text), muted = c(a.muted, b.muted), ink = c(a.ink, b.ink),
        surface = c(a.surface, b.surface), cardA = c(a.cardA, b.cardA), cardB = c(a.cardB, b.cardB),
        navA = c(a.navA, b.navA), navB = c(a.navB, b.navB),
        sessSydney = c(a.sessSydney, b.sessSydney), sessTokyo = c(a.sessTokyo, b.sessTokyo),
        sessLondon = c(a.sessLondon, b.sessLondon), sessNewYork = c(a.sessNewYork, b.sessNewYork),
        luxe = if (t < 0.5f) a.luxe else b.luxe,
        globe = if (t < 0.5f) a.globe else b.globe,
    )
}

/** Holds the live palette. Colours morph over ~650 ms when the theme changes. */
object ThemeState {
    private var from by mutableStateOf(Palettes.Default)
    private var to by mutableStateOf(Palettes.Default)
    private var progress by mutableFloatStateOf(1f)

    val palette: Palette get() = if (progress >= 1f) to else mix(from, to, progress)
    val target: Palette get() = to

    fun snap(p: Palette) {
        from = p
        to = p
        progress = 1f
    }

    suspend fun animateTo(p: Palette) {
        if (p == to) return
        from = palette
        to = p
        val a = Animatable(0f)
        progress = 0f
        a.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) { progress = value }
        progress = 1f
    }
}

/** Short colour names used across all screens. Every read follows the active theme. */
object Tx {
    val Bg: Color get() = ThemeState.palette.bg
    val Gold: Color get() = ThemeState.palette.gold
    val GoldHi: Color get() = ThemeState.palette.goldHi
    val GoldDeep: Color get() = ThemeState.palette.goldDeep
    val GoldPale: Color get() = ThemeState.palette.goldPale
    val Green: Color get() = ThemeState.palette.green
    val GreenDeep: Color get() = ThemeState.palette.greenDeep
    val Red: Color get() = ThemeState.palette.red
    val RedDeep: Color get() = ThemeState.palette.redDeep
    val Blue: Color get() = ThemeState.palette.blue
    val Purple: Color get() = ThemeState.palette.purple
    val Text: Color get() = ThemeState.palette.text
    val Muted: Color get() = ThemeState.palette.muted
    val Ink: Color get() = ThemeState.palette.ink
    val Surface: Color get() = ThemeState.palette.surface
    val CardA: Color get() = ThemeState.palette.cardA
    val CardB: Color get() = ThemeState.palette.cardB
    val NavA: Color get() = ThemeState.palette.navA
    val NavB: Color get() = ThemeState.palette.navB
    val SessSydney: Color get() = ThemeState.palette.sessSydney
    val SessTokyo: Color get() = ThemeState.palette.sessTokyo
    val SessLondon: Color get() = ThemeState.palette.sessLondon
    val SessNewYork: Color get() = ThemeState.palette.sessNewYork
    val Luxe: Boolean get() = ThemeState.palette.luxe
    val Globe: GlobeStyle get() = ThemeState.palette.globe
}

/**
 * Forces RTL (Persian first), applies the selected theme and the text size.
 * Replace the forced direction with the locale-driven one once the language picker is wired.
 */
@Composable
fun TradenexTheme(content: @Composable () -> Unit) {
    val settings = AppState.settings
    val target = remember(settings.theme, settings.accent) { Palettes.resolve(settings.theme, settings.accent) }
    LaunchedEffect(target) { ThemeState.animateTo(target) }

    val p = ThemeState.palette
    val density = LocalDensity.current
    val k = when (settings.textScale) {
        0 -> 0.92f
        2 -> 1.12f
        else -> 1f
    }
    val scaled = remember(density, k) { Density(density.density, density.fontScale * k) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = p.gold,
            onPrimary = p.ink,
            background = p.bg,
            surface = p.bg,
            onBackground = p.text,
            onSurface = p.text,
        ),
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalDensity provides scaled,
        ) {
            content()
        }
    }
}
