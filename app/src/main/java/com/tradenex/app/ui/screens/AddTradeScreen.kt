package com.tradenex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Trade
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import kotlin.math.abs
import kotlin.math.round

private val symbols = listOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "BTCUSD", "NAS100")
private val lotPresets = listOf(0.01, 0.05, 0.1, 0.5, 1.0, 2.0)
private val amountPresets = listOf(50.0, 100.0, 250.0, 500.0)
private val emotions = listOf("آرام", "ترس", "طمع", "انتقام", "خسته")

private fun round2(v: Double) = round(v * 100) / 100

private fun stepLot(v: Double, dir: Int): Double {
    val step = when {
        v < 0.1 || (v == 0.1 && dir < 0) -> 0.01
        v < 1.0 || (v == 1.0 && dir < 0) -> 0.1
        else -> 0.5
    }
    return round2(v + dir * step).coerceIn(0.01, 50.0)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTradeScreen(nav: NavController) {
    val settings = AppState.settings
    var buy by remember { mutableStateOf(true) }
    var symbol by remember { mutableStateOf("XAUUSD") }
    var lot by remember { mutableStateOf(settings.defaultLot) }
    var win by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf(250.0) }
    var strategy by remember { mutableStateOf(AppState.strategies.firstOrNull { it.active }?.name ?: "") }
    var emotion by remember { mutableStateOf("آرام") }
    var tags by remember { mutableStateOf(setOf<String>()) }

    val signed = if (win) amount else -amount
    val animated by animateFloatAsState(signed.toFloat(), tween(450), label = "net")
    val resColor = if (win) Tx.Green else Tx.Red
    val riskUsd = settings.startBalance * settings.riskPercent / 100.0
    val overRisk = !win && amount > riskUsd

    Page(title = "ثبت معامله", onBack = { nav.popBackStack() }) {
        Reveal(0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SideButton("خرید (Buy)", true, buy, Tx.Green, Modifier.weight(1f)) { buy = true }
                SideButton("فروش (Sell)", false, !buy, Tx.Red, Modifier.weight(1f)) { buy = false }
            }
        }
        Reveal(1) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                symbols.forEach { TxChip(it, symbol == it, { symbol = it }) }
            }
        }
        Reveal(2) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("حجم معامله (لات)", color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                    Stepper(
                        valueText = String.format(java.util.Locale.US, "%.2f", lot),
                        onMinus = { lot = stepLot(lot, -1) },
                        onPlus = { lot = stepLot(lot, 1) },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lotPresets.forEach { TxChip(it.toString(), lot == it, { lot = it }) }
                    }
                }
            }
        }
        Reveal(3) {
            GlassCard(Modifier.fillMaxWidth(), border = resColor.copy(alpha = 0.5f)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultToggle("سود (Profit)", win, Tx.Green, Modifier.weight(1f)) { win = true }
                        ResultToggle("ضرر (Loss)", !win, Tx.Red, Modifier.weight(1f)) { win = false }
                    }
                    Spacer(Modifier.height(8.dp))
                    Stepper(
                        valueText = "$" + String.format(java.util.Locale.US, "%,.2f", amount),
                        onMinus = { amount = (amount - 25).coerceAtLeast(0.0) },
                        onPlus = { amount += 25 },
                        valueColor = resColor,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        amountPresets.forEach { p ->
                            TxChip("+${p.toInt()}", false, { amount += p })
                        }
                    }
                }
            }
        }
        Reveal(4) {
            GlassCard(Modifier.fillMaxWidth(), border = resColor.copy(alpha = 0.5f)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نتیجه خالص", color = Tx.Muted, fontSize = 11.sp)
                        Text(Analytics.money(animated.toDouble()), color = resColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.weight(1f)) {
                        RowLabelCompact("هر لات", Analytics.money(signed / lot), resColor)
                        RowLabelCompact("از بالانس", String.format(java.util.Locale.US, "%+.2f%%", signed / settings.startBalance * 100), resColor)
                        RowLabelCompact("ریسک مجاز", "$" + String.format(java.util.Locale.US, "%,.0f", riskUsd), Tx.Muted)
                    }
                }
            }
        }
        if (overRisk) {
            Text(
                "ضرر از ریسک تعیین‌شده (${settings.riskPercent}٪ بالانس) بیشتر است؛ در دفترچه علامت می‌خورد.",
                color = Tx.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Reveal(5) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("استراتژی", color = Tx.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppState.strategies.filter { it.active }.forEach { TxChip(it.name, strategy == it.name, { strategy = it.name }) }
                }
                Text("حال ذهنی هنگام معامله", color = Tx.Muted, fontSize = 11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    emotions.forEach { TxChip(it, emotion == it, { emotion = it }) }
                }
                Text("هشتگ‌ها", color = Tx.Muted, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppState.hashtags.forEach { h ->
                        TxChip(h, h in tags, { tags = if (h in tags) tags - h else tags + h })
                    }
                }
            }
        }
        Reveal(6) {
            GoldButton("ذخیره معامله در دفترچه") {
                AppState.addTrade(
                    Trade(
                        id = System.currentTimeMillis(),
                        symbol = symbol,
                        buy = buy,
                        lot = lot,
                        pnl = signed,
                        strategy = strategy,
                        tags = tags.toList(),
                        epochDay = AppState.today(),
                        emotion = emotion,
                    ),
                )
                nav.popBackStack()
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SideButton(label: String, isBuy: Boolean, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(if (selected) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(if (isBuy) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown, null, tint = if (selected) Tx.Ink else Tx.Muted)
        Text(label, color = if (selected) Tx.Ink else Tx.Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ResultToggle(label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) color else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) color else Tx.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RowLabelCompact(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = Tx.Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
