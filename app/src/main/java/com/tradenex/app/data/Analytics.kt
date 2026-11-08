package com.tradenex.app.data

import com.tradenex.app.core.NativeCore

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
    /** Computed by the Rust core when available (same maths in the Kotlin fallback). */
    fun candles(trades: List<Trade>, period: Period, startBalance: Double): List<Candle> {
        val sorted = trades.sortedBy { it.epochDay }
        val rows = NativeCore.candles(
            DoubleArray(sorted.size) { sorted[it].pnl },
            LongArray(sorted.size) { period.bucket(sorted[it].epochDay) },
            startBalance,
            period.count,
        )
        return rows.map { Candle(it[0], it[1], it[2], it[3]) }
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
