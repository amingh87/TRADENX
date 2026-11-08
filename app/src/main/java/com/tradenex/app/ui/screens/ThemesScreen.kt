package com.tradenex.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.SessionEngine
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.AccentPicker
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.SpinningGlobe
import com.tradenex.app.ui.components.ThemeCard
import com.tradenex.app.ui.theme.AppTheme
import com.tradenex.app.ui.theme.Tx

@Composable
fun ThemesScreen(nav: NavController) {
    val s = AppState.settings
    val utc by rememberUtcMinute()
    val mask = SessionEngine.states(utc, 0).foldIndexed(0) { i, m, st -> if (st.open) m or (1 shl i) else m }

    Page(title = "تم‌ها", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            Text("ظاهر برنامه را انتخاب کن. تغییر تم با انیمیشن نرم انجام می‌شود و همه صفحه‌ها، کره زمین و نمودارها هماهنگ می‌شوند.", color = Tx.Muted, fontSize = 12.sp)
        }
        AppTheme.entries.forEachIndexed { i, theme ->
            Reveal(i + 1) {
                ThemeCard(theme, selected = s.theme == theme.ordinal, onClick = { AppState.update { it.copy(theme = theme.ordinal) } }, modifier = Modifier.fillMaxWidth())
            }
        }
        Reveal(3) {
            Column {
                SectionTitle("پیش‌نمایش زنده")
                Spacer(Modifier.height(6.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        SpinningGlobe(mask, utc, Modifier.fillMaxWidth(0.8f), s.animations)
                    }
                }
            }
        }
        Reveal(4) {
            SettingGroup("رنگ تأکیدی") {
                Text("جایگزین رنگ طلایی تم انتخاب‌شده می‌شود.", color = Tx.Muted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                AccentPicker(s.accent) { v -> AppState.update { it.copy(accent = v) } }
            }
        }
        Reveal(5) {
            SettingGroup("افکت‌ها") {
                SwitchRow("ذرات طلایی", "غبار طلایی شناور در پس‌زمینه", s.particles) { v -> AppState.update { it.copy(particles = v) } }
                SwitchRow("جارو نور روی کارت‌ها", "فقط در تم GOLD", s.shimmer) { v -> AppState.update { it.copy(shimmer = v) } }
                Text("شدت انیمیشن‌ها", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("کم", "عادی", "فوق‌العاده"), s.animations) { v -> AppState.update { it.copy(animations = v) } }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
