package com.tradenex.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.data.Strategy
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.GoldButton
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.theme.Tx

@Composable
fun StrategiesScreen(nav: NavController) {
    var name by remember { mutableStateOf("") }
    var ruleInput by remember { mutableStateOf("") }
    val rules = remember { mutableStateListOf<String>() }
    val n = AppState.trades.size

    Page(title = "استراتژی‌های من", onBack = { nav.popBackStack() }) {
        // comparison
        Reveal(0) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("کدام استراتژی برایت کار می‌کند؟")
                    val nets = AppState.strategies.map { s -> s to AppState.trades.filter { it.strategy == s.name }.sumOf { it.pnl } }
                    val maxAbs = nets.maxOfOrNull { kotlin.math.abs(it.second) }?.coerceAtLeast(1.0) ?: 1.0
                    nets.forEach { (s, net) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, color = Tx.Text, fontSize = 11.sp, modifier = Modifier.width(96.dp))
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f))) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((kotlin.math.abs(net) / maxAbs).toFloat().coerceIn(0.03f, 1f))
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (net >= 0) Tx.Green else Tx.Red),
                                )
                            }
                            Text(Analytics.money(net), color = if (net >= 0) Tx.Green else Tx.Red, fontSize = 11.sp, modifier = Modifier.width(78.dp).padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        AppState.strategies.forEachIndexed { idx, s ->
            Reveal(idx + 1) { StrategyCard(s, n) }
        }

        Reveal(AppState.strategies.size + 1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lightbulb, null, tint = Tx.Gold)
                        Text("  استراتژی جدید", color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    TxField(name, { name = it }, "نام استراتژی (مثلاً بریک‌اوت لندن)")
                    Text("قانون‌های ورود (چک‌لیست قبل از هر معامله)", color = Tx.Muted, fontSize = 11.sp)
                    rules.forEachIndexed { i, r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Check, null, tint = Tx.Green, modifier = Modifier.size(18.dp))
                            Text("  $r", color = Tx.Text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.Delete, "حذف قانون", tint = Tx.Muted, modifier = Modifier.size(20.dp).clickable { rules.removeAt(i) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TxField(ruleInput, { ruleInput = it }, "افزودن قانون...", Modifier.weight(1f))
                        IconBox(Icons.Rounded.Add, "افزودن قانون") {
                            if (ruleInput.isNotBlank()) {
                                rules.add(ruleInput.trim())
                                ruleInput = ""
                            }
                        }
                    }
                    GoldButton("ذخیره استراتژی") {
                        if (name.isNotBlank()) {
                            AppState.strategies.add(Strategy(System.currentTimeMillis(), name.trim(), rules.toList(), true))
                            name = ""
                            rules.clear()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StrategyCard(s: Strategy, tradeCount: Int) {
    val mine = remember(s.name, tradeCount) { AppState.trades.filter { it.strategy == s.name } }
    val net = mine.sumOf { it.pnl }
    val win = if (mine.isEmpty()) 0.0 else mine.count { it.pnl > 0 }.toDouble() / mine.size
    val curve = remember(s.name, tradeCount) {
        var eq = 0.0
        mine.takeLast(24).map { eq += it.pnl; eq }
    }
    val c = if (net >= 0) Tx.Green else Tx.Red
    GlassCard(Modifier.fillMaxWidth(), border = if (s.active) Tx.Gold.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.12f)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(s.name, color = if (s.active) Tx.Text else Tx.Muted, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("${s.rules.size} قانون · ${mine.size} معامله", color = Tx.Muted, fontSize = 11.sp)
                Text(s.rules.take(2).joinToString(" · "), color = Tx.Muted, fontSize = 10.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spark(curve, c, Modifier.width(72.dp).height(24.dp))
                Text(Analytics.money(net), color = c, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text("برد ${(win * 100).toInt()}%", color = Tx.Muted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwitchMini(s.active) {
                    val i = AppState.strategies.indexOfFirst { it.id == s.id }
                    if (i >= 0) AppState.strategies[i] = s.copy(active = !s.active)
                }
                Icon(
                    Icons.Rounded.Delete,
                    "حذف استراتژی",
                    tint = Tx.Muted,
                    modifier = Modifier.size(22.dp).clickable { AppState.strategies.removeAll { it.id == s.id } },
                )
            }
        }
    }
}

@Composable
private fun SwitchMini(on: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (on) Color(0xFF14B888) else Color(0xFF4A4A52))
            .clickable(onClick = onToggle)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.White))
    }
}

@Composable
private fun Spark(values: List<Double>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = values.min()
        val hi = values.max()
        val span = (hi - lo).coerceAtLeast(1e-6)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - ((v - lo) / span).toFloat() * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
