package com.tradenex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.core.NativeCore
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Period
import com.tradenex.app.ui.components.CandleChart
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.StatTile
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun PerformanceScreen(nav: NavController) {
    val settings = AppState.settings
    var period by remember { mutableStateOf(Period.MONTH) }
    val n = AppState.trades.size
    val today = AppState.today()
    val windowDays = when (period) {
        Period.DAY -> 7
        Period.WEEK -> 30
        Period.MONTH -> 90
        Period.YEAR -> 365
    }
    val windowed = remember(period, n) { AppState.trades.filter { it.epochDay > today - windowDays } }
    val stats = remember(period, n) { NativeCore.stats(windowed.map { it.pnl }.toDoubleArray()) }
    val candles = remember(period, n) { Analytics.candles(AppState.trades, period, settings.startBalance) }
    val dayPnl = remember(n) { Analytics.dayPnl(AppState.trades) }
    val inWindow = dayPnl.filterKeys { it > today - windowDays }
    val best = inWindow.values.maxOrNull() ?: 0.0
    val worst = inWindow.values.minOrNull() ?: 0.0
    val noTrade = Analytics.noTradeDays(AppState.trades, today, 35, settings.weekendHoliday)
    val netColor = if (stats.net >= 0) Tx.Green else Tx.Red

    Page(title = "عملکرد من") {
        Reveal(0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Period.entries.forEach { TxChip(it.label, period == it, { period = it }, Modifier) }
            }
        }
        Reveal(1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("سود خالص $windowDays روز اخیر", color = Tx.Muted, fontSize = 11.sp)
                            Text(
                                if (settings.hideBalance) "•••" else String.format(java.util.Locale.US, "%+.2f%%", stats.net / settings.startBalance * 100),
                                color = netColor,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Text(if (settings.hideBalance) "••••" else Analytics.money(stats.net), color = netColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    CandleChart(candles, Modifier.fillMaxWidth().height(140.dp))
                }
            }
        }
        Reveal(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("معاملات", windowed.size.toString(), Tx.Text, Modifier.weight(1f))
                StatTile("نرخ برد", String.format(java.util.Locale.US, "%.0f%%", stats.winRate * 100), Tx.Gold, Modifier.weight(1f))
                StatTile("ضریب سود", String.format(java.util.Locale.US, "%.2f", stats.profitFactor), Tx.Green, Modifier.weight(1f))
                StatTile("افت سرمایه", String.format(java.util.Locale.US, "%,.0f", stats.maxDrawdown), Tx.Red, Modifier.weight(1f))
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("نقشه حرارتی ۵ هفته اخیر", "شنبه تا جمعه")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("شنبه", "یک", "دوش", "سه", "چهار", "پنج", "جمعه", "جمع").forEach {
                            Text(it, color = Tx.Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                    val weekStart = today - Analytics.column(today)
                    for (r in 0..4) {
                        val start = weekStart - 7L * (4 - r)
                        var sum = 0.0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (c in 0..6) {
                                val d = start + c
                                val pnl = dayPnl[d]
                                if (pnl != null) sum += pnl
                                HeatCell(
                                    day = LocalDate.ofEpochDay(d).dayOfMonth,
                                    pnl = pnl,
                                    holiday = settings.weekendHoliday && c < 2,
                                    future = d > today,
                                    index = r * 7 + c,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                (if (sum >= 0) "+" else "-") + abs(sum).toInt(),
                                color = if (sum >= 0) Tx.Green else Tx.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f).padding(top = 10.dp),
                            )
                        }
                    }
                }
            }
        }
        Reveal(4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("بهترین روز", Analytics.money(best), Tx.Green, Modifier.weight(1f))
                StatTile("بدترین روز", Analytics.money(worst), Tx.Red, Modifier.weight(1f))
                StatTile("بدون معامله", "$noTrade روز", Tx.Gold, Modifier.weight(1f), sub = "۵ هفته اخیر")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HeatCell(day: Int, pnl: Double?, holiday: Boolean, future: Boolean, index: Int, modifier: Modifier) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 18L)
        on = true
    }
    val a by animateFloatAsState(if (on) 1f else 0f, tween(400), label = "cell")
    val shape = RoundedCornerShape(9.dp)
    val (bg, fg) = when {
        holiday -> Color.Transparent to Tx.Muted.copy(alpha = 0.5f)
        future -> Color.Transparent to Tx.Muted.copy(alpha = 0.35f)
        pnl == null -> Color.White.copy(alpha = 0.10f) to Tx.Muted
        pnl >= 0 -> (if (pnl > 150) Tx.Green else Tx.GreenDeep) to (if (pnl > 150) Tx.Ink else Color(0xFFE9FFF6))
        else -> (if (pnl < -120) Tx.RedDeep else Color(0xFF6B2530)) to Color.White
    }
    Box(
        modifier
            .height(36.dp)
            .graphicsLayer {
                alpha = a
                scaleX = 0.6f + 0.4f * a
                scaleY = 0.6f + 0.4f * a
            }
            .clip(shape)
            .background(bg)
            .then(if (holiday) Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(day.toString(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
