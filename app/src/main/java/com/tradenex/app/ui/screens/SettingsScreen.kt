package com.tradenex.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
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
import com.tradenex.app.core.GlobeData
import com.tradenex.app.core.NativeCore
import com.tradenex.app.ui.components.AccentPicker
import com.tradenex.app.ui.theme.AppTheme
import com.tradenex.app.data.AppSettings
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.theme.Tx
import java.util.Locale
import kotlin.math.round

private fun update(block: (AppSettings) -> AppSettings) = AppState.update(block)

@Composable
fun SettingsScreen(nav: NavController) {
    val s = AppState.settings
    var confirmClear by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Page(title = "تنظیمات", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            SettingGroup("تم‌ها") {
                Text("تم برنامه", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(AppTheme.entries.map { it.label }, s.theme) { v -> update { it.copy(theme = v) } }
                Spacer(Modifier.height(10.dp))
                Text("رنگ تأکیدی", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                AccentPicker(s.accent) { v -> update { it.copy(accent = v) } }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clickable { nav.navigate("themes") }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("گالری تم‌ها و پیش‌نمایش زنده", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("‹", color = Tx.Gold, fontSize = 18.sp)
                }
            }
        }
        Reveal(1) {
            SettingGroup("افکت‌ها و نمایش") {
                SwitchRow("ذرات طلایی", "غبار شناور در پس‌زمینه", s.particles) { v -> update { it.copy(particles = v) } }
                SwitchRow("جارو نور روی کارت‌ها", "فقط در تم GOLD", s.shimmer) { v -> update { it.copy(shimmer = v) } }
                SwitchRow("سایه شب و روز روی کره", "ترمیناتور بر اساس ساعت UTC", s.globeNight) { v -> update { it.copy(globeNight = v) } }
                SwitchRow("چراغ شهرها و نام سشن‌ها", "روی کره GOLD", s.globeCities) { v -> update { it.copy(globeCities = v) } }
                Text("اندازه متن", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("کوچک", "عادی", "بزرگ"), s.textScale) { v -> update { it.copy(textScale = v) } }
                Spacer(Modifier.height(10.dp))
                Text("گردی گوشه کارت‌ها", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("تیز", "عادی", "گرد"), s.cardRadius) { v -> update { it.copy(cardRadius = v) } }
            }
        }
        Reveal(2) {
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
        Reveal(3) {
            SettingGroup("سشن‌ها") {
                SwitchRow("نمایش کره زمین", "خاموش = نوار زمانی", s.showGlobe) { v -> update { it.copy(showGlobe = v) } }
                Text("منطقه زمانی نمایش", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ChipsRow(listOf("تهران", "UTC", "ساعت دستگاه"), s.tzMode) { v -> update { it.copy(tzMode = v) } }
                SwitchRow("هشدار باز/بسته‌شدن سشن", null, s.sessionAlerts) { v -> update { it.copy(sessionAlerts = v) } }
            }
        }
        Reveal(4) {
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
        Reveal(5) {
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
        Reveal(6) {
            SettingGroup("داده و پشتیبان") {
                SwitchRow("پشتیبان‌گیری خودکار", "به‌زودی", s.autoBackup) { v -> update { it.copy(autoBackup = v) } }
                InfoRow("تعداد معامله‌ها", "${AppState.trades.size}")
                InfoRow("تعداد استراتژی‌ها", "${AppState.strategies.size}")
                InfoRow("تعداد هشتگ‌ها", "${AppState.hashtags.size}")
                InfoRow("موتور محاسبات", if (NativeCore.isNative) "Rust (JNI)" else "Kotlin", if (NativeCore.isNative) Tx.Green else Tx.Gold)
                InfoRow("نقاط کره زمین", if (GlobeData.ready) "${GlobeData.dotCount} نقطه" else "در انتظار بارگذاری")
                InfoRow("نسخه", "0.2.0")
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Tx.Red.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable { confirmClear = true },
                    contentAlignment = Alignment.Center,
                ) { Text("پاک‌کردن همه معامله‌ها", color = Tx.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Reveal(7) {
            GoldButton("بازنشانی همه تنظیمات") { confirmReset = true }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Tx.Surface,
            title = { Text("پاک‌کردن معامله‌ها؟", color = Tx.Text) },
            text = { Text("همه ${AppState.trades.size} معامله حذف می‌شود و برگشت ندارد.", color = Tx.Muted) },
            confirmButton = { TextButton(onClick = { AppState.clearTrades(); confirmClear = false }) { Text("حذف", color = Tx.Red) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Tx.Surface,
            title = { Text("بازنشانی تنظیمات؟", color = Tx.Text) },
            text = { Text("همه تنظیمات (از جمله تم) به حالت پیش‌فرض برمی‌گردد. معامله‌ها دست نمی‌خورند.", color = Tx.Muted) },
            confirmButton = { TextButton(onClick = { update { AppSettings() }; confirmReset = false }) { Text("بازنشانی", color = Tx.Gold) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, color: androidx.compose.ui.graphics.Color = Tx.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Stepper(value, onMinus, onPlus)
    }
}
