package com.tradenex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection

object Tx {
    val Bg = Color(0xFF07070A)
    val Gold = Color(0xFFF7CF6B)
    val GoldDeep = Color(0xFFC98E1E)
    val GoldPale = Color(0xFFFFE9A8)
    val Green = Color(0xFF2BE8A8)
    val GreenDeep = Color(0xFF0C7A56)
    val Red = Color(0xFFFF6B7A)
    val RedDeep = Color(0xFFB9283A)
    val Blue = Color(0xFF4DA3FF)
    val Purple = Color(0xFFB58CFF)
    val Text = Color(0xFFF4EFE3)
    val Muted = Color(0xFFB4AE9F)
    val Ink = Color(0xFF1A1204)
}

/** Forces RTL (Persian first). Replace with the locale-driven direction once the language picker is wired. */
@Composable
fun TradenexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Tx.Gold,
            onPrimary = Tx.Ink,
            background = Tx.Bg,
            surface = Tx.Bg,
            onBackground = Tx.Text,
            onSurface = Tx.Text,
        ),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            content()
        }
    }
}
