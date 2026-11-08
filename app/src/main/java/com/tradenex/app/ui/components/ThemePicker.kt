package com.tradenex.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.ui.theme.AppTheme
import com.tradenex.app.ui.theme.Palette
import com.tradenex.app.ui.theme.Palettes
import com.tradenex.app.ui.theme.Tx

/** A theme tile with a miniature preview drawn from the theme's own palette (not the active one). */
@Composable
fun ThemeCard(theme: AppTheme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = Palettes.of(theme.ordinal)
    val pop by animateFloatAsState(
        if (selected) 1.02f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pop",
    )
    val shape = RoundedCornerShape(cardRadius())
    Column(
        modifier
            .scale(pop)
            .clip(shape)
            .background(Tx.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Tx.Gold else Tx.Muted.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick),
    ) {
        ThemePreview(p, Modifier.fillMaxWidth().height(120.dp))
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(theme.label, color = Tx.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text(theme.tagline, color = Tx.Muted, fontSize = 11.sp)
            }
            Box(
                Modifier.size(26.dp).clip(CircleShape)
                    .background(if (selected) Tx.Gold else Color.Transparent)
                    .border(1.5.dp, if (selected) Tx.Gold else Tx.Muted, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Tx.Ink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ThemePreview(p: Palette, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(p.bg)
        drawCircle(Brush.radialGradient(listOf(p.gold.copy(alpha = 0.30f), Color.Transparent), Offset(size.width * 0.85f, 0f), size.width * 0.8f), size.width * 0.8f, Offset(size.width * 0.85f, 0f))
        val pad = 10.dp.toPx()
        val cardW = size.width - pad * 2
        val cardH = size.height - pad * 2
        val rad = CornerRadius(12.dp.toPx())
        // card
        if (p.luxe) {
            drawRoundRect(Brush.linearGradient(listOf(p.cardA, p.cardB)), Offset(pad, pad), Size(cardW, cardH), rad)
        } else {
            drawRoundRect(Color.White.copy(alpha = 0.07f), Offset(pad, pad), Size(cardW, cardH), rad)
        }
        drawRoundRect(p.gold.copy(alpha = 0.55f), Offset(pad, pad), Size(cardW, cardH), rad, style = Stroke(1.dp.toPx()))
        // mini globe on the left
        val gc = Offset(pad + cardH * 0.5f, size.height / 2)
        val gr = cardH * 0.36f
        if (p.luxe) {
            drawCircle(Brush.radialGradient(listOf(p.goldDeep.copy(alpha = 0.6f), p.surface, p.bg), Offset(gc.x - gr * 0.3f, gc.y - gr * 0.3f), gr * 1.6f), gr, gc)
            for (i in 0 until 44) {
                val a = i * 2.399f
                val rr = gr * 0.9f * kotlin.math.sqrt((i + 1) / 45f)
                drawCircle(p.gold.copy(alpha = 0.8f), 1.3.dp.toPx(), Offset(gc.x + rr * kotlin.math.cos(a), gc.y + rr * kotlin.math.sin(a)))
            }
        } else {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1D5FBF), Color(0xFF0C2C66), Color(0xFF040C22)), Offset(gc.x - gr * 0.3f, gc.y - gr * 0.3f), gr * 1.5f), gr, gc)
            for (i in 0 until 44) {
                val a = i * 2.399f
                val rr = gr * 0.9f * kotlin.math.sqrt((i + 1) / 45f)
                drawCircle(Color(0xFF7FE3D0).copy(alpha = 0.8f), 1.3.dp.toPx(), Offset(gc.x + rr * kotlin.math.cos(a), gc.y + rr * kotlin.math.sin(a)))
            }
        }
        drawCircle(p.gold.copy(alpha = 0.6f), gr, gc, style = Stroke(1.dp.toPx()))
        // candles on the right
        val hs = floatArrayOf(0.30f, 0.50f, 0.38f, 0.66f, 0.55f, 0.82f)
        val up = booleanArrayOf(true, true, false, true, false, true)
        val x0 = pad + cardH + 14.dp.toPx()
        val step = (cardW - cardH - 24.dp.toPx()) / hs.size
        for (i in hs.indices) {
            val h = cardH * 0.7f * hs[i]
            val x = x0 + i * step
            val col = if (up[i]) p.green else p.red
            drawRoundRect(col, Offset(x, size.height - pad - 8.dp.toPx() - h), Size(step * 0.55f, h), CornerRadius(2.dp.toPx()))
        }
    }
}

/** Row of round colour swatches for the accent colour. */
@Composable
fun AccentPicker(selected: Int, onSelect: (Int) -> Unit) {
    val swatches = listOf(Color(0xFFF2B53D), Color(0xFF2BE8A8), Color(0xFFB58CFF))
    val names = listOf("طلایی", "زمردی", "بنفش")
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        swatches.forEachIndexed { i, c ->
            val on = i == selected
            val sc by animateFloatAsState(if (on) 1.12f else 1f, label = "sw")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(i) }) {
                Box(
                    Modifier.size(36.dp).scale(sc).clip(CircleShape).background(c)
                        .border(if (on) 3.dp else 1.dp, if (on) Tx.Text else Tx.Muted.copy(alpha = 0.4f), CircleShape),
                )
                Spacer(Modifier.height(4.dp))
                Text(names[i], color = if (on) Tx.Text else Tx.Muted, fontSize = 10.sp)
            }
        }
    }
}
