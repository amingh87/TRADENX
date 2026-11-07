package com.tradenex.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.AppSettings
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.theme.Tx
import java.util.Locale
import kotlin.math.round

private fun update(block: (AppSettings) -> AppSettings) {
    AppState.settings = block(AppState.settings)
}

@Composable
fun SettingsScreen(nav: NavController) {
    val s = AppState.settings

    Page(title = "تنظیمات", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            SettingGroup("ظاهر و زبان") {
                Text("زبان برنامه", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("فارسی", "English"), s.language) { v -> update { it.copy(language = v) } }
                Spacer(Modifier.height(10.dp))
                Text("شدت انیمیشن‌ها", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("کم", "عادی", "فوق‌العاده"), s.animations) { v -> update { it.copy(animations = v) } }
                SwitchRow("لرزش لمسی", "بازخورد هنگام ثبت معامله", s.haptics) { v -> update { it.copy(haptics = v) } }
                SwitchRow("پنهان‌کردن مبالغ", "اعداد پول در خانه و گزارش‌ها نقطه‌چین می‌شود", s.hideBalance) { v -> update { it.copy(hideBalance = v) } }
            }
        }
        Reveal(1) {
            SettingGroup("سشن‌ها") {
                SwitchRow("نمایش کره زمین", "خاموش = نوار زمانی", s.showGlobe) { v -> update { it.copy(showGlobe = v) } }
                Text("منطقه زمانی نمایش", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("تهران", "UTC", "ساعت دستگاه"), s.tzMode) { v -> update { it.copy(tzMode = v) } }
                SwitchRow("هشدار باز/بسته‌شدن سشن", null, s.sessionAlerts) { v -> update { it.copy(sessionAlerts = v) } }
            }
        }
        Reveal(2) {
            SettingGroup("معامله و ریسک") {
                StepRow("بالانس اولیه", "$" + String.format(Locale.US, "%,.0f", s.startBalance),
                    { update { it.copy(startBalance = (it.startBalance - 500).coerceAtLeast(100.0)) } },
                    { update { it.copy(startBalance = it.startBalance + 500) } })
                StepRow("لات پیش‌فرض", String.format(Locale.US, "%.2f", s.defaultLot),
                    { update { it.copy(defaultLot = (round((it.defaultLot - 0.05) * 100) / 100).coerceAtLeast(0.01)) } },
                    { update { it.copy(defaultLot = round((it.defaultLot + 0.05) * 100) / 100) } })
                StepRow("ریسک هر معامله", String.format(Locale.US, "%.2f%%", s.riskPercent),
                    { update { it.copy(riskPercent = (it.riskPercent - 0.25).coerceAtLeast(0.25)) } },
                    { update { it.copy(riskPercent = (it.riskPercent + 0.25).coerceAtMost(10.0)) } })
                SwitchRow("شنبه و یکشنبه تعطیل", "در نقشه حرارتی و تقویم خط‌خورده می‌شود", s.weekendHoliday) { v -> update { it.copy(weekendHoliday = v) } }
            }
        }
        Reveal(3) {
            SettingGroup("یادآوری و اخبار") {
                SwitchRow("یادآور روز بدون معامله", "می‌پرسد چرا امروز معامله نکردی", s.reminderOn) { v -> update { it.copy(reminderOn = v) } }
                StepRow("ساعت یادآور", "%02d:00".format(s.reminderHour),
                    { update { it.copy(reminderHour = (it.reminderHour - 1).coerceAtLeast(0)) } },
                    { update { it.copy(reminderHour = (it.reminderHour + 1).coerceAtMost(23)) } })
                SwitchRow("هشدار اخبار مهم", null, s.newsAlerts) { v -> update { it.copy(newsAlerts = v) } }
                Text("حداقل اهمیت خبر", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("همه", "متوسط به بالا", "فقط High"), s.newsImpact) { v -> update { it.copy(newsImpact = v) } }
            }
        }
        Reveal(4) {
            SettingGroup("داده و پشتیبان") {
                SwitchRow("پشتیبان‌گیری خودکار", "به‌زودی", s.autoBackup) { v -> update { it.copy(autoBackup = v) } }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("موتور محاسبات", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(if (NativeCore.isNative) "Rust (JNI)" else "Kotlin", color = if (NativeCore.isNative) Tx.Green else Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("نسخه", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("0.1.0", color = Tx.Text, fontSize = 12.sp)
                }
            }
        }
        Reveal(5) {
            GoldButton("بازنشانی همه تنظیمات") { AppState.settings = AppSettings() }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StepRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Stepper(value, onMinus, onPlus)
    }
}
