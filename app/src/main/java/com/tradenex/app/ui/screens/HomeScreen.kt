package com.tradenex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.SessionEngine
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Period
import com.tradenex.app.ui.components.CandleChart
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.PulseDot
import com.tradenex.app.ui.components.SessionTimeline
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx

@Composable
fun HomeScreen(nav: NavController) {
    val settings = AppState.settings
    val utc by rememberUtcMinute()
    val tz = SessionEngine.tzOffsetMinutes(settings.tzMode)
    val tradeCount = AppState.trades.size
    val stats = remember(tradeCount) { AppState.stats() }
    val today = AppState.today()
    val monthPnl = AppState.trades.filter { it.epochDay > today - 30 }.sumOf { it.pnl }
    val monthPct = monthPnl / settings.startBalance * 100
    val grossWin = AppState.trades.filter { it.pnl > 0 }.sumOf { it.pnl }
    val grossLoss = AppState.trades.filter { it.pnl < 0 }.sumOf { it.pnl }
    val todayCount = AppState.trades.count { it.epochDay == today }
    var period by remember { mutableStateOf(Period.WEEK) }

    val shownPct by animateFloatAsState(monthPct.toFloat(), tween(1200), label = "pct")
    val sessions = SessionEngine.states(utc, tz)
    val nowLocal = (utc + tz) % 1440
    val openCount = sessions.count { it.open }
    val activity = listOf(15, 45, 72, 95)[minOf(openCount, 3)]

    Column(Modifier.fillMaxSize()) {
        // top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundBtn(Icons.Rounded.Person, "پروفایل") { nav.navigate("more") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TRADENEX",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.verticalGradient(listOf(Tx.GoldPale, Color(0xFFE7A92B))),
                    ),
                )
                Text("Trade · Journal · Analyze · Improve", color = Tx.Muted, fontSize = 9.sp)
            }
            RoundBtn(Icons.Rounded.Notifications, "اعلان‌ها") { nav.navigate("settings") }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Reveal(0) {
                GlassCard(Modifier.fillMaxWidth().height(140.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        // glowing orbits on the leading edge of the hero card
                        val c = Offset(size.width * 0.16f, size.height * 0.5f)
                        for (i in 0..2) {
                            drawCircle(
                                Tx.Gold.copy(alpha = 0.25f - i * 0.06f),
                                radius = 30.dp.toPx() + i * 16.dp.toPx(),
                                center = c,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()),
                            )
                        }
                        drawCircle(
                            Brush.radialGradient(listOf(Tx.Gold, Color(0xFF8A5A12), Color(0xFF1A1204)), center = c, radius = 30.dp.toPx()),
                            radius = 26.dp.toPx(),
                            center = c,
                        )
                    }
                    Column(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), horizontalAlignment = Alignment.End) {
                        Text("عملکرد ۳۰ روز اخیر", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        val col = if (monthPct >= 0) Tx.Green else Tx.Red
                        val shown = if (settings.hideBalance) "•••" else String.format(java.util.Locale.US, "%+.2f%%", shownPct)
                        Text(shown, color = col, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (settings.hideBalance) "••••" else Analytics.money(monthPnl) + " USD",
                            color = col,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Reveal(1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("نرخ برد", "%.1f%%".format(stats.winRate * 100), Tx.Gold, Modifier.weight(1f))
                    StatTile("نرخ باخت", "%.1f%%".format((1 - stats.winRate) * 100), Tx.Red, Modifier.weight(1f))
                    StatTile("مجموع سود", if (settings.hideBalance) "•••" else "+%,.0f".format(grossWin), Tx.Green, Modifier.weight(1f))
                    StatTile("مجموع ضرر", if (settings.hideBalance) "•••" else "%,.0f".format(grossLoss), Tx.Red, Modifier.weight(1f))
                }
            }

            Reveal(2) {
                GlassCard(Modifier.fillMaxWidth().clickable { nav.navigate("sessions") }) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PulseDot(if (openCount > 0) Tx.Green else Tx.Red)
                                Text("  وضعیت سشن‌ها", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            SessionTimeline(sessions, nowLocal, Modifier.fillMaxWidth().height(40.dp))
                            Spacer(Modifier.height(6.dp))
                            val overlap = SessionEngine.overlaps(utc).firstOrNull { it.activeNow }
                            Text(
                                if (overlap != null) "همپوشانی فعال: ${overlap.a} + ${overlap.b}"
                                else sessions.firstOrNull { it.open }?.let { "${it.def.name} باز · ${SessionEngine.duration(it.minutesToChange)} تا بسته‌شدن" }
                                    ?: "همه سشن‌ها بسته‌اند",
                                color = Tx.Gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            ActivityRing(activity / 100f)
                            Text("فعالیت بازار", color = Tx.Text, fontSize = 11.sp)
                        }
                    }
                }
            }

            Reveal(3) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("عملکرد کندلی", color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Period.entries.forEach {
                                TxChip(it.label, period == it, { period = it }, Modifier.padding(start = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val candles = remember(period, tradeCount) {
                            Analytics.candles(AppState.trades, period, settings.startBalance)
                        }
                        CandleChart(candles, Modifier.fillMaxWidth().height(110.dp))
                    }
                }
            }

            if (todayCount == 0 && settings.reminderOn) {
                Reveal(4) {
                    GlassCard(Modifier.fillMaxWidth(), border = Tx.Red.copy(alpha = 0.45f)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Notifications, null, tint = Tx.Gold)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text("امروز هنوز معامله‌ای ثبت نشده", color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("ساعت %02d:00 می‌پرسم چرا".format(settings.reminderHour), color = Tx.Muted, fontSize = 11.sp)
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD978), Tx.GoldDeep)))
                                    .clickable { nav.navigate("add") }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) { Text("ثبت", color = Tx.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }
                }
            }

            Reveal(5) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Quick("ثبت معامله", Icons.Rounded.Add, Modifier.weight(1f)) { nav.navigate("add") }
                        Quick("استراتژی‌ها", Icons.Rounded.TrackChanges, Modifier.weight(1f)) { nav.navigate("strategies") }
                        Quick("آموزش", Icons.Rounded.MenuBook, Modifier.weight(1f)) { nav.navigate("edu") }
                        Quick("تقویم", Icons.Rounded.CalendarMonth, Modifier.weight(1f)) { nav.navigate("calendar") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Quick("سشن‌ها", Icons.Rounded.Public, Modifier.weight(1f)) { nav.navigate("sessions") }
                        Quick("گزارش‌ها", Icons.Rounded.BarChart, Modifier.weight(1f)) { nav.navigate("perf") }
                        Quick("هشتگ‌ها", Icons.Rounded.Tag, Modifier.weight(1f)) { nav.navigate("tags") }
                        Quick("تنظیمات", Icons.Rounded.Settings, Modifier.weight(1f)) { nav.navigate("settings") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.5.dp, Tx.Gold, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Tx.Gold) }
}

@Composable
private fun Quick(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .height(58.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Tx.Gold.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Tx.Gold, modifier = Modifier.size(20.dp))
        Text(label, color = Tx.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ActivityRing(fraction: Float) {
    val f by animateFloatAsState(fraction, tween(1100), label = "ring")
    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val sz = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.1f), 0f, 360f, false, Offset(inset, inset), sz, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
            drawArc(Tx.Green, -90f, 360f * f, false, Offset(inset, inset), sz, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(f * 100).toInt()}%", color = Tx.Text, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
