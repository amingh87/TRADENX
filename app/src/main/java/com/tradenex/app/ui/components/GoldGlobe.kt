package com.tradenex.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.core.GlobeData
import com.tradenex.app.core.NativeCore
import com.tradenex.app.core.SessionEngine
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.theme.Tx
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private class Hub(val name: String, val lat: Double, val lon: Double, val session: Int)

private val hubs = listOf(
    Hub("Sydney", -33.9, 151.2, 0),
    Hub("Tokyo", 35.7, 139.7, 1),
    Hub("London", 51.5, -0.1, 2),
    Hub("New York", 40.7, -74.0, 3),
)

private fun unit3(latDeg: Double, lonDeg: Double): DoubleArray {
    val la = Math.toRadians(latDeg)
    val lo = Math.toRadians(lonDeg)
    return doubleArrayOf(cos(la) * sin(lo), sin(la), cos(la) * cos(lo))
}

/** Same rotation as rust/src/globe.rs: (lon0, lat0) faces the viewer. Returns x, y, depth. */
private fun rot(v: DoubleArray, lon0: Double, lat0: Double, k: Double = 1.0): DoubleArray {
    val cl = cos(Math.toRadians(lon0)); val sl = sin(Math.toRadians(lon0))
    val cp = cos(Math.toRadians(lat0)); val sp = sin(Math.toRadians(lat0))
    val x1 = v[0] * cl - v[2] * sl
    val z1 = v[0] * sl + v[2] * cl
    val y2 = v[1] * cp - z1 * sp
    val z2 = v[1] * sp + z1 * cp
    return doubleArrayOf(x1 * k, y2 * k, z2 * k)
}

/**
 * The GOLD theme globe: ~4.4k land dots projected by the Rust core every frame,
 * day/night terminator, city lights, glowing session regions, pulse rings, trade arcs,
 * an orbiting satellite and labels.
 */
@Composable
fun GoldGlobe(openMask: Int, utcMinute: Int, modifier: Modifier = Modifier, speed: Int = 1) {
    val ctx = LocalContext.current
    var ready by remember { mutableIntStateOf(if (GlobeData.ready) 1 else 0) }
    LaunchedEffect(Unit) {
        GlobeData.ensure(ctx)
        ready = if (GlobeData.ready) 1 else 0
    }
    val settings = AppState.settings
    val measurer = rememberTextMeasurer()
    val t = rememberInfiniteTransition(label = "goldglobe")
    val spinMs = when (speed) {
        0 -> 120000
        2 -> 28000
        else -> 60000
    }
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(spinMs, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "pulse")
    val dash by t.animateFloat(0f, 40f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "dash")
    val sway by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "sway")

    val sunLon = (12.0 - utcMinute / 60.0) * 15.0
    val sun = remember(utcMinute) {
        val u = unit3(-15.5, sunLon)
        floatArrayOf(u[0].toFloat(), u[1].toFloat(), u[2].toFloat())
    }
    val buf = remember(ready) { FloatArray(NativeCore.GLOBE_STRIDE * (GlobeData.dotCount + 8)) }
    val label = remember { TextStyle(color = Color.White, fontSize = 9.sp) }

    Canvas(modifier.aspectRatio(1f)) {
        val c = center
        val r = size.minDimension * 0.40f
        val lon0 = 20.0 + spin
        val lat0 = 22.0 + 6.0 * sway

        // orbit ring (back half) and atmosphere
        rotate(-18f, c) {
            drawOval(Tx.Gold.copy(alpha = 0.28f), Offset(c.x - r * 1.28f, c.y - r * 0.34f), Size(r * 2.56f, r * 0.68f), style = Stroke(1.2.dp.toPx()))
        }
        drawCircle(
            Brush.radialGradient(listOf(Color.Transparent, Tx.Gold.copy(alpha = 0.40f), Color.Transparent), center = c, radius = r * 1.24f),
            radius = r * 1.24f, center = c,
        )
        // body: warm black sphere with a gold glow low-left
        drawCircle(
            Brush.radialGradient(
                listOf(Tx.GoldDeep.copy(alpha = 0.55f), Tx.Surface, Tx.Bg),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.35f), radius = r * 1.6f,
            ),
            radius = r, center = c,
        )

        // graticule
        val grat = Tx.Gold.copy(alpha = 0.13f)
        for (latDeg in intArrayOf(-60, -30, 0, 30, 60)) {
            var prev: Offset? = null
            var lonDeg = -180
            while (lonDeg <= 180) {
                val p = rot(unit3(latDeg.toDouble(), lonDeg.toDouble()), lon0, lat0)
                val o = if (p[2] > 0) Offset(c.x + (p[0] * r).toFloat(), c.y - (p[1] * r).toFloat()) else null
                if (prev != null && o != null) drawLine(grat, prev, o, 0.7.dp.toPx())
                prev = o
                lonDeg += 6
            }
        }
        for (lonDeg in -150..180 step 30) {
            var prev: Offset? = null
            var latDeg = -90
            while (latDeg <= 90) {
                val p = rot(unit3(latDeg.toDouble(), lonDeg.toDouble()), lon0, lat0)
                val o = if (p[2] > 0) Offset(c.x + (p[0] * r).toFloat(), c.y - (p[1] * r).toFloat()) else null
                if (prev != null && o != null) drawLine(grat, prev, o, 0.7.dp.toPx())
                prev = o
                latDeg += 6
            }
        }

        // land dots (projected by Rust) -- [x, y, depth, day, tag, lit]
        if (ready == 1) {
            val n = NativeCore.globeProject(lon0.toFloat(), lat0.toFloat(), sun, settings.globeNight, buf)
            val base = 1.25.dp.toPx()
            val s = NativeCore.GLOBE_STRIDE
            for (i in 0 until n) {
                val o = i * s
                val depth = buf[o + 2]
                val day = buf[o + 3]
                val tag = buf[o + 4].toInt()
                val lit = buf[o + 5]
                val pos = Offset(c.x + buf[o] * r, c.y - buf[o + 1] * r)
                val shade = (0.35f + 0.65f * depth)
                when {
                    tag in 1..4 -> {
                        val def = SessionEngine.defs[tag - 1]
                        val on = (openMask shr (tag - 1)) and 1 == 1
                        val a = (if (on) 0.95f else 0.45f) * shade * (0.5f + 0.5f * day)
                        drawCircle(def.color.copy(alpha = a.coerceIn(0f, 1f)), base * (if (on) 1.5f else 1.1f), pos)
                    }
                    settings.globeCities && lit > 0.25f && day < 0.45f -> {
                        val a = (0.45f + 0.55f * lit) * shade
                        drawCircle(Tx.GoldPale.copy(alpha = a.coerceIn(0f, 1f)), base * 1.35f, pos)
                        drawCircle(Tx.Gold.copy(alpha = 0.16f * lit), base * 3.2f, pos)
                    }
                    else -> {
                        val a = (0.20f + 0.70f * day) * shade
                        drawCircle(Tx.Gold.copy(alpha = a.coerceIn(0f, 1f)), base, pos)
                    }
                }
            }
        }

        // trade-flow arcs
        for ((ia, ib) in listOf(2 to 3, 2 to 1, 1 to 0)) {
            val a = unit3(hubs[ia].lat, hubs[ia].lon)
            val b = unit3(hubs[ib].lat, hubs[ib].lon)
            val path = androidx.compose.ui.graphics.Path()
            var started = false
            for (st in 0..28) {
                val f = st / 28.0
                val m = doubleArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)
                val len = sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2])
                if (len < 1e-6) continue
                val lift = 1.0 + 0.16 * sin(PI * f)
                val p = rot(doubleArrayOf(m[0] / len, m[1] / len, m[2] / len), lon0, lat0, lift)
                if (p[2] > 0) {
                    val x = c.x + (p[0] * r).toFloat(); val y = c.y - (p[1] * r).toFloat()
                    if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                } else {
                    started = false
                }
            }
            drawPath(
                path, SessionEngine.defs[hubs[ia].session].color.copy(alpha = 0.85f),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), dash)),
            )
        }

        // hubs: pulse rings when open, always a core and a label
        for (h in hubs) {
            val p = rot(unit3(h.lat, h.lon), lon0, lat0)
            if (p[2] <= 0.05) continue
            val o = Offset(c.x + (p[0] * r).toFloat(), c.y - (p[1] * r).toFloat())
            val col = SessionEngine.defs[h.session].color
            val open = (openMask shr h.session) and 1 == 1
            if (open) {
                drawCircle(col.copy(alpha = 0.55f * (1f - pulse)), (6f + 20f * pulse).dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
                drawCircle(col.copy(alpha = 0.22f), 13.dp.toPx(), o)
            }
            drawCircle(Color.White, (if (open) 5.2f else 3.6f).dp.toPx(), o)
            drawCircle(col, (if (open) 3.8f else 2.6f).dp.toPx(), o)
            if (settings.globeCities) {
                val m = measurer.measure(h.name, label.copy(color = if (open) Tx.Text else Tx.Muted))
                drawText(m, topLeft = Offset(o.x - m.size.width / 2f, o.y + 7.dp.toPx()))
            }
        }

        // rim light + satellite on the orbit
        drawCircle(Tx.Gold.copy(alpha = 0.55f), r, c, style = Stroke(1.4.dp.toPx()))
        val ang = spin * 2f * PI / 180.0
        rotate(-18f, c) {
            val sx = c.x + (cos(ang) * r * 1.28).toFloat()
            val sy = c.y + (sin(ang) * r * 0.34).toFloat()
            drawCircle(Tx.Gold.copy(alpha = 0.3f), 8.dp.toPx(), Offset(sx, sy))
            drawCircle(Tx.GoldPale, 3.2.dp.toPx(), Offset(sx, sy))
        }
    }
}
