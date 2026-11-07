package com.tradenex.app.core

import androidx.compose.ui.graphics.Color
import com.tradenex.app.ui.theme.Tx
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone

data class SessionDef(val name: String, val startUtc: Int, val endUtc: Int, val color: Color)

data class SessionState(
    val def: SessionDef,
    val open: Boolean,
    val minutesToChange: Int,
    val localStart: Int,
    val length: Int,
)

data class OverlapInfo(val a: String, val b: String, val minutes: Int, val percent: Int, val activeNow: Boolean)

object SessionEngine {
    val defs = listOf(
        SessionDef("Sydney", 1320, 420, Tx.Blue),
        SessionDef("Tokyo", 0, 540, Tx.Purple),
        SessionDef("London", 480, 1020, Tx.Gold),
        SessionDef("New York", 780, 1320, Tx.Red),
    )

    fun currentUtcMinute(): Int {
        val z = ZonedDateTime.now(ZoneOffset.UTC)
        return z.hour * 60 + z.minute
    }

    /** 0 = Tehran (UTC+3:30), 1 = UTC, 2 = device zone. */
    fun tzOffsetMinutes(mode: Int): Int = when (mode) {
        0 -> 210
        1 -> 0
        else -> TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
    }

    fun states(utcMinute: Int, tzOffset: Int): List<SessionState> {
        val mask = NativeCore.sessionMask(utcMinute)
        return defs.mapIndexed { i, d ->
            SessionState(
                def = d,
                open = (mask shr i) and 1 == 1,
                minutesToChange = NativeCore.minutesToChange(utcMinute, i),
                localStart = (((d.startUtc + tzOffset) % 1440) + 1440) % 1440,
                length = ((d.endUtc - d.startUtc) + 1440) % 1440,
            )
        }
    }

    fun overlaps(utcMinute: Int): List<OverlapInfo> {
        val mask = NativeCore.sessionMask(utcMinute)
        return listOf(0 to 1, 1 to 2, 2 to 3).map { (a, b) ->
            val m = NativeCore.overlapMinutes(a, b)
            OverlapInfo(
                a = defs[a].name,
                b = defs[b].name,
                minutes = m,
                percent = m * 100 / 540,
                activeNow = (mask shr a) and 1 == 1 && (mask shr b) and 1 == 1,
            )
        }
    }

    fun clock(minute: Int): String {
        val m = ((minute % 1440) + 1440) % 1440
        return "%02d:%02d".format(m / 60, m % 60)
    }

    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m دقیقه"
            m == 0 -> "$h ساعت"
            else -> "$h ساعت و $m دقیقه"
        }
    }
}
