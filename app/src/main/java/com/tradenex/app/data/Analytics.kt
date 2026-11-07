package com.tradenex.app.data

import kotlin.math.max
import kotlin.math.min

data class Candle(val o: Double, val h: Double, val l: Double, val c: Double)

/** Equity-curve candles are built per bucket of [bucket] days. Day 0 (1970-01-01) was a Thursday. */
enum class Period(val label: String, val count: Int) {
    DAY("روزانه", 24),
    WEEK("هفتگی", 16),
    MONTH("ماهانه", 12),
    YEAR("سالانه", 3);

    fun bucket(epochDay: Long): Long = when (this) {
        DAY -> epochDay
        WEEK -> (epochDay + 5) / 7
        MONTH -> epochDay / 30
        YEAR -> epochDay / 365
    }
}

object Analytics {
    fun candles(trades: List<Trade>, period: Period, startBalance: Double): List<Candle> {
        val groups = trades.sortedBy { it.epochDay }.groupBy { period.bucket(it.epochDay) }
        var equity = startBalance
        val out = ArrayList<Candle>()
        for ((_, list) in groups) {
            val open = equity
            var hi = equity
            var lo = equity
            for (t in list) {
                equity += t.pnl
                hi = max(hi, equity)
                lo = min(lo, equity)
            }
            out.add(Candle(open, hi, lo, equity))
        }
        return out.takeLast(period.count)
    }

    fun dayPnl(trades: List<Trade>): Map<Long, Double> =
        trades.groupBy { it.epochDay }.mapValues { (_, v) -> v.sumOf { it.pnl } }

    /** Saturday-first column index: 0 Sat, 1 Sun, 2 Mon ... 6 Fri. */
    fun column(epochDay: Long): Int = ((epochDay + 5) % 7).toInt()

    fun noTradeDays(trades: List<Trade>, today: Long, lookback: Int, weekendHoliday: Boolean): Int {
        val days = trades.map { it.epochDay }.toSet()
        var n = 0
        for (d in (today - lookback + 1)..today) {
            if (weekendHoliday && column(d) < 2) continue
            if (d !in days) n++
        }
        return n
    }

    fun money(v: Double): String =
        (if (v >= 0) "+" else "-") + "$" + String.format(java.util.Locale.US, "%,.2f", kotlin.math.abs(v))
}
