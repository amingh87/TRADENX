package com.tradenex.app.core

/** [net], [winRate] (0..1), [profitFactor], [maxDrawdown] (<= 0). */
data class TradeStats(
    val net: Double,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
)

/**
 * Bridge to the Rust library `libtradenex_core.so` (see /rust).
 * When the library is not bundled the identical Kotlin fallback is used,
 * so the app always works; [isNative] tells which engine is active.
 */
object NativeCore {
    private val loaded: Boolean = try {
        System.loadLibrary("tradenex_core")
        true
    } catch (_: Throwable) {
        false
    }

    val isNative: Boolean get() = loaded

    private external fun nSessionMask(minuteUtc: Int): Int
    private external fun nOverlapMinutes(a: Int, b: Int): Int
    private external fun nMinutesToChange(minuteUtc: Int, session: Int): Int
    private external fun nStats(pnls: DoubleArray): DoubleArray
    private external fun nGlobeLoad(bytes: ByteArray): Int
    private external fun nGlobeProject(lon: Float, lat: Float, sx: Float, sy: Float, sz: Float, night: Boolean, out: FloatArray): Int
    private external fun nCandles(pnls: DoubleArray, buckets: LongArray, start: Double, keep: Int): DoubleArray

    fun sessionMask(minuteUtc: Int): Int =
        if (loaded) nSessionMask(minuteUtc) else Fallback.sessionMask(minuteUtc)

    fun overlapMinutes(a: Int, b: Int): Int =
        if (loaded) nOverlapMinutes(a, b) else Fallback.overlapMinutes(a, b)

    fun minutesToChange(minuteUtc: Int, session: Int): Int =
        if (loaded) nMinutesToChange(minuteUtc, session) else Fallback.minutesToChange(minuteUtc, session)

    /** Floats written per visible dot by [globeProject]: x, y, depth, day, tag, lit. */
    const val GLOBE_STRIDE = 6

    private var nativeGlobe = false

    /** Hand the world-dots asset to the engine. Returns the dot count (0 if invalid). */
    fun globeLoad(bytes: ByteArray): Int {
        nativeGlobe = false
        if (loaded) {
            val n = try { nGlobeLoad(bytes) } catch (_: Throwable) { -1 }
            if (n > 0) {
                nativeGlobe = true
                return n
            }
        }
        return Fallback.globeLoad(bytes)
    }

    /** Rotate, cull and shade the dots. Returns how many dots were written to [out]. */
    fun globeProject(lon: Float, lat: Float, sun: FloatArray, night: Boolean, out: FloatArray): Int =
        if (nativeGlobe) nGlobeProject(lon, lat, sun[0], sun[1], sun[2], night, out)
        else Fallback.globeProject(lon, lat, sun, night, out)

    /** Candle rows [open, high, low, close] of the equity curve. */
    fun candles(pnls: DoubleArray, buckets: LongArray, start: Double, keep: Int): List<DoubleArray> {
        val flat = if (loaded) nCandles(pnls, buckets, start, keep) else Fallback.candles(pnls, buckets, start, keep)
        return List(flat.size / 4) { i -> doubleArrayOf(flat[i * 4], flat[i * 4 + 1], flat[i * 4 + 2], flat[i * 4 + 3]) }
    }

    fun stats(pnls: DoubleArray): TradeStats {
        val r = if (loaded) nStats(pnls) else Fallback.stats(pnls)
        return TradeStats(r[0], r[1], r[2], r[3])
    }
}

/** Same maths as rust/src/lib.rs. */
internal object Fallback {
    // start, end in UTC minutes: Sydney, Tokyo, London, New York
    private val sessions = arrayOf(intArrayOf(1320, 420), intArrayOf(0, 540), intArrayOf(480, 1020), intArrayOf(780, 1320))

    private fun isOpen(minute: Int, start: Int, end: Int): Boolean =
        if (start < end) minute in start until end else (minute >= start || minute < end)

    fun sessionMask(minuteUtc: Int): Int {
        val m = ((minuteUtc % 1440) + 1440) % 1440
        var mask = 0
        for (i in sessions.indices) if (isOpen(m, sessions[i][0], sessions[i][1])) mask = mask or (1 shl i)
        return mask
    }

    fun overlapMinutes(a: Int, b: Int): Int {
        if (a !in 0..3 || b !in 0..3) return 0
        var n = 0
        for (m in 0 until 1440) {
            if (isOpen(m, sessions[a][0], sessions[a][1]) && isOpen(m, sessions[b][0], sessions[b][1])) n++
        }
        return n
    }

    fun minutesToChange(minuteUtc: Int, session: Int): Int {
        if (session !in 0..3) return 0
        val now = ((minuteUtc % 1440) + 1440) % 1440
        val open = isOpen(now, sessions[session][0], sessions[session][1])
        for (step in 1..1440) {
            if (isOpen((now + step) % 1440, sessions[session][0], sessions[session][1]) != open) return step
        }
        return 0
    }

    fun stats(pnls: DoubleArray): DoubleArray {
        if (pnls.isEmpty()) return doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        var net = 0.0
        var gain = 0.0
        var loss = 0.0
        var wins = 0
        var equity = 0.0
        var peak = 0.0
        var dd = 0.0
        for (p in pnls) {
            net += p
            if (p > 0) {
                gain += p
                wins++
            } else {
                loss += -p
            }
            equity += p
            if (equity > peak) peak = equity
            if (equity - peak < dd) dd = equity - peak
        }
        val pf = if (loss > 0) gain / loss else if (gain > 0) 99.0 else 0.0
        return doubleArrayOf(net, wins.toDouble() / pnls.size, pf, dd)
    }

    // ---- Globe dots (same maths as rust/src/globe.rs) ----
    private var gx = FloatArray(0)
    private var gy = FloatArray(0)
    private var gz = FloatArray(0)
    private var gTag = ByteArray(0)
    private var gLit = ByteArray(0)

    fun globeLoad(b: ByteArray): Int {
        if (b.size < 8 || b[0] != 'T'.code.toByte() || b[1] != 'X'.code.toByte() || b[2] != 'W'.code.toByte() || b[3] != '1'.code.toByte()) return 0
        val n = (b[4].toInt() and 255) or ((b[5].toInt() and 255) shl 8) or ((b[6].toInt() and 255) shl 16) or ((b[7].toInt() and 255) shl 24)
        if (n <= 0 || b.size < 8 + n * 6) return 0
        gx = FloatArray(n); gy = FloatArray(n); gz = FloatArray(n)
        gTag = ByteArray(n); gLit = ByteArray(n)
        fun i16(o: Int): Int = ((b[o].toInt() and 255) or (b[o + 1].toInt() shl 8)).toShort().toInt()
        for (i in 0 until n) {
            val o = 8 + i * 6
            val lo = Math.toRadians(i16(o) / 100.0)
            val la = Math.toRadians(i16(o + 2) / 100.0)
            gx[i] = (Math.cos(la) * Math.sin(lo)).toFloat()
            gy[i] = Math.sin(la).toFloat()
            gz[i] = (Math.cos(la) * Math.cos(lo)).toFloat()
            gTag[i] = b[o + 4]
            gLit[i] = b[o + 5]
        }
        return n
    }

    fun globeProject(lon: Float, lat: Float, sun: FloatArray, night: Boolean, out: FloatArray): Int {
        val lr = Math.toRadians(lon.toDouble()); val pr = Math.toRadians(lat.toDouble())
        val sl = Math.sin(lr).toFloat(); val cl = Math.cos(lr).toFloat()
        val sp = Math.sin(pr).toFloat(); val cp = Math.cos(pr).toFloat()
        val cap = out.size / 6
        var n = 0
        for (i in gx.indices) {
            val x = gx[i]; val y = gy[i]; val z = gz[i]
            val x1 = x * cl - z * sl
            val z1 = x * sl + z * cl
            val y2 = y * cp - z1 * sp
            val z2 = y * sp + z1 * cp
            if (z2 <= 0.02f) continue
            if (n >= cap) break
            val day = if (night) (0.5f + (x * sun[0] + y * sun[1] + z * sun[2]) * 3f).coerceIn(0f, 1f) else 1f
            val o = n * 6
            out[o] = x1; out[o + 1] = y2; out[o + 2] = z2; out[o + 3] = day
            out[o + 4] = (gTag[i].toInt() and 255).toFloat()
            out[o + 5] = (gLit[i].toInt() and 255) / 255f
            n++
        }
        return n
    }

    fun candles(pnls: DoubleArray, buckets: LongArray, start: Double, keep: Int): DoubleArray {
        val n = minOf(pnls.size, buckets.size)
        val out = ArrayList<Double>()
        var eq = start
        var i = 0
        while (i < n) {
            val key = buckets[i]
            val open = eq
            var hi = eq
            var lo = eq
            while (i < n && buckets[i] == key) {
                eq += pnls[i]
                if (eq > hi) hi = eq
                if (eq < lo) lo = eq
                i++
            }
            out.add(open); out.add(hi); out.add(lo); out.add(eq)
        }
        val skip = (out.size - keep.coerceAtLeast(0) * 4).coerceAtLeast(0)
        return out.drop(skip).toDoubleArray()
    }
}
