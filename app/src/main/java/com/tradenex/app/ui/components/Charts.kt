package com.tradenex.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.tradenex.app.core.SessionState
import com.tradenex.app.data.Candle
import com.tradenex.app.ui.theme.Tx
import kotlin.math.max
import kotlin.math.min

/** Equity candles that grow in from the middle line every time the data changes. */
@Composable
fun CandleChart(candles: List<Candle>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(candles) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        if (candles.isEmpty()) return@Canvas
        val hi = candles.maxOf { it.h }
        val lo = candles.minOf { it.l }
        val span = (hi - lo).coerceAtLeast(1e-6)
        val slot = size.width / candles.size
        val bodyW = slot * 0.6f
        val mid = size.height / 2f
        val p = progress.value

        fun y(v: Double): Float {
            val raw = size.height - (((v - lo) / span) * 0.9 + 0.05).toFloat() * size.height
            return mid + (raw - mid) * p
        }

        for (i in 1..3) {
            val gy = size.height * i / 4f
            drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, gy), Offset(size.width, gy), 1f)
        }
        candles.forEachIndexed { i, c ->
            val color = if (c.c >= c.o) Tx.Green else Tx.Red
            val x = slot * i + slot / 2f
            drawLine(color, Offset(x, y(c.h)), Offset(x, y(c.l)), 2.dp.toPx(), StrokeCap.Round)
            val top = y(max(c.o, c.c))
            val bottom = y(min(c.o, c.c))
            drawRoundRect(
                color,
                Offset(x - bodyW / 2f, top),
                Size(bodyW, (bottom - top).coerceAtLeast(3f)),
                CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/** 24h bars for the four sessions in the chosen time zone, with a white "now" marker. */
@Composable
fun SessionTimeline(states: List<SessionState>, nowLocalMinute: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val rowH = size.height / states.size
        val barH = (rowH * 0.55f).coerceAtMost(8.dp.toPx())
        states.forEachIndexed { i, s ->
            val y = rowH * i + (rowH - barH) / 2f
            val color = s.def.color.copy(alpha = if (s.open) 1f else 0.5f)
            val x0 = s.localStart / 1440f * size.width
            val w = s.length / 1440f * size.width
            val r = CornerRadius(barH / 2f)
            if (x0 + w <= size.width) {
                drawRoundRect(color, Offset(x0, y), Size(w, barH), r)
            } else {
                drawRoundRect(color, Offset(x0, y), Size(size.width - x0, barH), r)
                drawRoundRect(color, Offset(0f, y), Size(x0 + w - size.width, barH), r)
            }
        }
        val nx = nowLocalMinute / 1440f * size.width
        drawLine(Color.White, Offset(nx, 0f), Offset(nx, size.height), 2.dp.toPx())
    }
}
