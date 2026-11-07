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

    fun sessionMask(minuteUtc: Int): Int =
        if (loaded) nSessionMask(minuteUtc) else Fallback.sessionMask(minuteUtc)

    fun overlapMinutes(a: Int, b: Int): Int =
        if (loaded) nOverlapMinutes(a, b) else Fallback.overlapMinutes(a, b)

    fun minutesToChange(minuteUtc: Int, session: Int): Int =
        if (loaded) nMinutesToChange(minuteUtc, session) else Fallback.minutesToChange(minuteUtc, session)

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
}
