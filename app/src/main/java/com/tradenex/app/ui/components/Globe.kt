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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.tradenex.app.ui.theme.Tx
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private class City(val name: String, val lat: Double, val lon: Double, val color: Color, val session: Int)

private val cities = listOf(
    City("Sydney", -33.9, 151.2, Tx.Blue, 0),
    City("Tokyo", 35.7, 139.7, Tx.Purple, 1),
    City("London", 51.5, -0.1, Tx.Gold, 2),
    City("New York", 40.7, -74.0, Tx.Red, 3),
)
private const val TEHRAN_LAT = 35.7
private const val TEHRAN_LON = 51.4

// Coarse continent outlines (lat, lon). Good enough for a stylised dotted globe.
private val continents: List<List<Pair<Double, Double>>> = listOf(
    listOf(72.0 to -165.0, 72.0 to -100.0, 60.0 to -62.0, 46.0 to -53.0, 32.0 to -77.0, 25.0 to -80.0, 20.0 to -97.0, 16.0 to -92.0, 8.0 to -80.0, 18.0 to -105.0, 30.0 to -115.0, 48.0 to -125.0, 60.0 to -140.0),
    listOf(12.0 to -72.0, 8.0 to -60.0, -5.0 to -35.0, -23.0 to -41.0, -40.0 to -62.0, -55.0 to -68.0, -52.0 to -75.0, -18.0 to -71.0, -5.0 to -81.0, 8.0 to -77.0),
    listOf(71.0 to 25.0, 60.0 to 30.0, 55.0 to 38.0, 45.0 to 40.0, 40.0 to 28.0, 36.0 to 10.0, 36.0 to -9.0, 44.0 to -9.0, 50.0 to -5.0, 58.0 to -5.0, 62.0 to 5.0),
    listOf(37.0 to 10.0, 32.0 to 32.0, 12.0 to 43.0, -2.0 to 41.0, -26.0 to 33.0, -35.0 to 20.0, -18.0 to 12.0, 4.0 to 9.0, 5.0 to -8.0, 15.0 to -17.0, 30.0 to -10.0),
    listOf(75.0 to 100.0, 72.0 to 140.0, 60.0 to 165.0, 45.0 to 142.0, 35.0 to 130.0, 22.0 to 120.0, 8.0 to 105.0, 10.0 to 78.0, 22.0 to 70.0, 25.0 to 57.0, 30.0 to 48.0, 40.0 to 40.0, 45.0 to 50.0, 55.0 to 60.0, 60.0 to 70.0),
    listOf(-12.0 to 131.0, -12.0 to 142.0, -25.0 to 153.0, -38.0 to 146.0, -35.0 to 117.0, -22.0 to 114.0),
    listOf(83.0 to -35.0, 76.0 to -18.0, 60.0 to -43.0, 70.0 to -55.0),
)

private fun inPoly(lat: Double, lon: Double, poly: List<Pair<Double, Double>>): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val (yi, xi) = poly[i]
        val (yj, xj) = poly[j]
        if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

/** (lat, lon) in degrees of every land dot, computed once. */
private val landDots: List<Pair<Double, Double>> by lazy {
    val out = ArrayList<Pair<Double, Double>>()
    var lat = -58.0
    while (lat <= 80.0) {
        var lon = -180.0
        while (lon < 180.0) {
            if (continents.any { inPoly(lat, lon, it) }) out.add(lat to lon)
            lon += 4.0
        }
        lat += 4.0
    }
    out
}

private fun unit(lat: Double, lon: Double): DoubleArray {
    val la = lat * PI / 180
    val lo = lon * PI / 180
    return doubleArrayOf(cos(la) * cos(lo), cos(la) * sin(lo), sin(la))
}

/**
 * Spinning orthographic globe: real land dots, day/night shading from the UTC time,
 * pulsing markers for open sessions, animated trade-flow arcs and an orbiting satellite.
 *
 * @param openMask bit i = session i is open (0 Sydney, 1 Tokyo, 2 London, 3 New York)
 * @param utcMinute minute of the UTC day, used to place the sun
 * @param speed 0 = slow, 1 = normal, 2 = fast
 */
@Composable
fun SpinningGlobe(openMask: Int, utcMinute: Int, modifier: Modifier = Modifier, speed: Int = 1) {
    val t = rememberInfiniteTransition(label = "globe")
    val spinMs = when (speed) {
        0 -> 90000
        2 -> 22000
        else -> 45000
    }
    val spin by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(spinMs, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "pulse",
    )
    val dash by t.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "dash",
    )
    val sun = unit(-15.5, (12.0 - utcMinute / 60.0) * 15.0)

    Canvas(modifier.aspectRatio(1f)) {
        val c = center
        val r = size.minDimension * 0.40f
        val lon0 = 20.0 + spin
        val tilt = 0.35

        // lat/lon -> rotated vector (x right, y up, z towards viewer)
        fun rotated(v: DoubleArray, k: Double = 1.0): DoubleArray {
            val lat = asin(v[2].coerceIn(-1.0, 1.0))
            val lon = atan2(v[1], v[0]) * 180 / PI - lon0
            val lo = lon * PI / 180
            val x = cos(lat) * sin(lo)
            val y0 = sin(lat)
            val z0 = cos(lat) * cos(lo)
            val y = y0 * cos(tilt) - z0 * sin(tilt)
            val z = y0 * sin(tilt) + z0 * cos(tilt)
            return doubleArrayOf(x * k, y * k, z * k)
        }

        fun screen(p: DoubleArray): Offset = Offset(c.x + (p[0] * r).toFloat(), c.y - (p[1] * r).toFloat())

        // orbit ring behind the globe + atmosphere
        rotate(-18f, c) {
            drawOval(
                Tx.Gold.copy(alpha = 0.30f),
                Offset(c.x - r * 1.28f, c.y - r * 0.34f),
                Size(r * 2.56f, r * 0.68f),
                style = Stroke(1.2.dp.toPx()),
            )
        }
        drawCircle(
            Brush.radialGradient(
                listOf(Color.Transparent, Tx.Blue.copy(alpha = 0.45f), Color.Transparent),
                center = c,
                radius = r * 1.22f,
            ),
            radius = r * 1.22f,
            center = c,
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF1D5FBF), Color(0xFF0C2C66), Color(0xFF040C22)),
                center = Offset(c.x - r * 0.3f, c.y - r * 0.3f),
                radius = r * 1.5f,
            ),
            radius = r,
            center = c,
        )

        // graticule
        for (latDeg in listOf(-60, -30, 0, 30, 60)) {
            var prev: Offset? = null
            var lonDeg = -180
            while (lonDeg <= 180) {
                val p = rotated(unit(latDeg.toDouble(), lonDeg.toDouble()))
                val o = if (p[2] > 0) screen(p) else null
                if (prev != null && o != null) drawLine(Color(0xFF7FB8FF).copy(alpha = 0.22f), prev, o, 0.8.dp.toPx())
                prev = o
                lonDeg += 6
            }
        }

        // land dots with night shading
        for ((lat, lon) in landDots) {
            val v = unit(lat, lon)
            val p = rotated(v)
            if (p[2] <= 0.02) continue
            val day = max(0.0, v[0] * sun[0] + v[1] * sun[1] + v[2] * sun[2])
            val a = (0.25 + 0.75 * day).toFloat() * (0.4f + 0.6f * p[2].toFloat())
            drawCircle(Color(0xFF7FE3D0).copy(alpha = a.coerceIn(0f, 1f)), 1.5.dp.toPx(), screen(p))
        }

        // trade-flow arcs
        val arcs = listOf(2 to 3, 2 to 1, 1 to 0)
        for ((ia, ib) in arcs) {
            val a = unit(cities[ia].lat, cities[ia].lon)
            val b = unit(cities[ib].lat, cities[ib].lon)
            var prev: Offset? = null
            val path = androidx.compose.ui.graphics.Path()
            for (s in 0..28) {
                val f = s / 28.0
                val m = doubleArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)
                val len = sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2])
                if (len < 1e-6) continue
                val lift = 1.0 + 0.14 * sin(PI * f)
                val p = rotated(doubleArrayOf(m[0] / len, m[1] / len, m[2] / len), lift)
                val o = if (p[2] > 0) screen(p) else null
                if (o != null) {
                    if (prev == null) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                prev = o
            }
            drawPath(
                path,
                cities[ia].color.copy(alpha = 0.85f),
                style = Stroke(1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), dash)),
            )
        }

        // cities
        for (city in cities) {
            val p = rotated(unit(city.lat, city.lon))
            if (p[2] <= 0) continue
            val o = screen(p)
            val open = (openMask shr city.session) and 1 == 1
            if (open) {
                drawCircle(city.color.copy(alpha = 0.55f * (1f - pulse)), (6f + 18f * pulse).dp.toPx(), o, style = Stroke(1.5.dp.toPx()))
                drawCircle(city.color.copy(alpha = 0.25f), 14.dp.toPx(), o)
            }
            drawCircle(Color.White, (if (open) 5.5f else 3.8f).dp.toPx(), o)
            drawCircle(city.color, (if (open) 4f else 2.8f).dp.toPx(), o)
        }
        // you are here (Tehran)
        val th = rotated(unit(TEHRAN_LAT, TEHRAN_LON))
        if (th[2] > 0) {
            val o = screen(th)
            drawCircle(Tx.Green.copy(alpha = 0.5f * (1f - pulse)), (4f + 12f * pulse).dp.toPx(), o, style = Stroke(1.2.dp.toPx()))
            drawCircle(Tx.Green, 3.2.dp.toPx(), o)
        }

        // rim light and a satellite on the orbit
        drawCircle(Tx.Blue.copy(alpha = 0.55f), r, c, style = Stroke(1.5.dp.toPx()))
        val ang = (spin * 2f) * PI / 180.0
        rotate(-18f, c) {
            drawCircle(
                Tx.Gold,
                3.5.dp.toPx(),
                Offset(c.x + (cos(ang) * r * 1.28).toFloat(), c.y + (sin(ang) * r * 0.34).toFloat()),
            )
        }
    }
}
