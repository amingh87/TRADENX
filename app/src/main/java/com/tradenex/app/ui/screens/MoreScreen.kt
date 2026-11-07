package com.tradenex.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.theme.Tx

private data class MenuItem(val title: String, val sub: String, val icon: ImageVector, val route: String, val color: Color)

@Composable
fun MoreScreen(nav: NavController) {
    var about by remember { mutableStateOf(false) }
    val items = listOf(
        MenuItem("سشن‌ها", "کره زمین و همپوشانی", Icons.Rounded.Public, "sessions", Tx.Blue),
        MenuItem("استراتژی‌ها", "ثبت، حذف و مقایسه", Icons.Rounded.TrackChanges, "strategies", Tx.Gold),
        MenuItem("تقویم و اخبار", "معاملات + فارکس فکتوری", Icons.Rounded.CalendarMonth, "calendar", Tx.Green),
        MenuItem("# هشتگ‌ها", "برچسب‌گذاری معامله‌ها", Icons.Rounded.Tag, "tags", Tx.Purple),
        MenuItem("گزارش‌ها", "کندل، نقشه حرارتی", Icons.Rounded.BarChart, "perf", Tx.Gold),
        MenuItem("آموزش", "متن، عکس، ویدیو", Icons.Rounded.MenuBook, "edu", Tx.Green),
        MenuItem("تنظیمات", "همه جزئیات برنامه", Icons.Rounded.Settings, "settings", Tx.Muted),
        MenuItem("درباره", "نسخه و هسته", Icons.Rounded.Info, "about", Tx.Blue),
    )
    val pulse = rememberInfiniteTransition(label = "crown")
    val s by pulse.animateFloat(1f, 1.1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "s")

    Page(title = "بیشتر") {
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).border(2.dp, Tx.Gold, CircleShape).background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Person, null, tint = Tx.Gold, modifier = Modifier.size(30.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("ترید‌ر حرفه‌ای", color = Tx.Text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${AppState.trades.size} معامله · " + if (NativeCore.isNative) "هسته Rust فعال" else "هسته Kotlin",
                            color = Tx.Muted,
                            fontSize = 11.sp,
                        )
                    }
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Tx.Gold, modifier = Modifier.size(30.dp).scale(s))
                }
            }
        }
        items.chunked(2).forEachIndexed { r, row ->
            Reveal(r + 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { m ->
                        MenuTile(m, Modifier.weight(1f)) { if (m.route == "about") about = true else nav.navigate(m.route) }
                    }
                }
            }
        }
        Reveal(6) {
            GlassCard(Modifier.fillMaxWidth(), border = Tx.Gold) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TRADENEX Pro", color = Tx.Gold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("تحلیل عمیق‌تر، پشتیبان‌گیری و بدون محدودیت", color = Tx.Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFD978), Tx.GoldDeep)))
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                    ) { Text("ارتقا به نسخه حرفه‌ای", color = Tx.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (about) {
        AlertDialog(
            onDismissRequest = { about = false },
            containerColor = Color(0xFF14141A),
            title = { Text("TRADENEX 0.1.0", color = Tx.Text) },
            text = {
                Text(
                    "دفترچه معاملات و آموزش فارکس. این برنامه سیگنال نمی‌دهد و معامله انجام نمی‌دهد.\nهسته محاسبات: " +
                        if (NativeCore.isNative) "Rust (JNI)" else "Kotlin (کتابخانه Rust بسته‌بندی نشده)",
                    color = Tx.Muted,
                )
            },
            confirmButton = { TextButton(onClick = { about = false }) { Text("باشه", color = Tx.Gold) } },
        )
    }
}

@Composable
private fun MenuTile(m: MenuItem, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier.height(92.dp).clickable(onClick = onClick), border = m.color.copy(alpha = 0.5f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(m.icon, null, tint = m.color, modifier = Modifier.size(26.dp))
            Column {
                Text(m.title, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(m.sub, color = Tx.Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun TagsScreen(nav: NavController) {
    var input by remember { mutableStateOf("") }
    val n = AppState.trades.size

    Page(title = "# هشتگ‌ها", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            Text(
                "هر معامله را با هشتگ برچسب بزن تا ببینی کدام موقعیت‌ها سود یا ضرر می‌آورند.",
                color = Tx.Muted,
                fontSize = 12.sp,
            )
        }
        Reveal(1) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxField(input, { input = it }, "هشتگ جدید (مثلاً asia_range)", Modifier.weight(1f))
                IconBox(Icons.Rounded.Add, "افزودن هشتگ") {
                    val t = input.trim().removePrefix("#").lowercase().replace(" ", "_")
                    if (t.isNotEmpty() && "#$t" !in AppState.hashtags) AppState.hashtags.add("#$t")
                    input = ""
                }
            }
        }
        AppState.hashtags
            .map { tag -> Triple(tag, AppState.trades.filter { tag in it.tags }, 0) }
            .sortedByDescending { it.second.size }
            .forEachIndexed { i, (tag, used, _) ->
                Reveal(i + 2) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tag, color = Tx.Purple, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${used.size} معامله از $n", color = Tx.Muted, fontSize = 11.sp)
                            }
                            val net = used.sumOf { it.pnl }
                            Text(Analytics.money(net), color = if (net >= 0) Tx.Green else Tx.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                Icons.Rounded.Delete,
                                "حذف هشتگ",
                                tint = Tx.Muted,
                                modifier = Modifier.padding(start = 12.dp).size(22.dp).clickable { AppState.hashtags.remove(tag) },
                            )
                        }
                    }
                }
            }
        Spacer(Modifier.height(12.dp))
    }
}
