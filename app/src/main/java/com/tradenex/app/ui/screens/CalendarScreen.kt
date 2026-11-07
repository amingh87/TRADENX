package com.tradenex.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.Analytics
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val jalaliMonths = listOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

/** Gregorian -> Jalali (year, month, day). */
fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return Triple(jy, jm, jd)
}

private fun fa(n: Int): String = n.toString().map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")

fun jalaliText(d: LocalDate): String {
    val (y, m, day) = toJalali(d.year, d.monthValue, d.dayOfMonth)
    return "${fa(day)} ${jalaliMonths[m - 1]} ${fa(y)}"
}

private data class NewsEvent(val id: Int, val time: String, val cur: String, val title: String, val impact: Int)

// Sample data. The real feed (ForexFactory) will replace this.
private val sampleNews = listOf(
    NewsEvent(1, "17:00", "USD", "FOMC Meeting Minutes", 2),
    NewsEvent(2, "18:30", "EUR", "ECB President Speaks", 1),
    NewsEvent(3, "19:30", "GBP", "BoE Governor Speaks", 1),
    NewsEvent(4, "21:30", "USD", "Non-Farm Payrolls", 2),
    NewsEvent(5, "22:00", "XAU", "Gold Inventory Report", 0),
)

@Composable
fun CalendarScreen(nav: NavController) {
    var tab by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(0) }
    var filter by remember { mutableStateOf(0) }
    var remind by remember { mutableStateOf(setOf<Int>()) }
    val today = LocalDate.now()
    val ym = YearMonth.from(today).plusMonths(offset.toLong())
    val n = AppState.trades.size
    val dayPnl = remember(n) { Analytics.dayPnl(AppState.trades) }
    val settings = AppState.settings

    Page(title = "تقویم", onBack = { nav.popBackStack() }, trailing = {
        TxChip("امروز", false, { offset = 0 })
    }) {
        Reveal(0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxChip("تقویم معامله‌ها", tab == 0, { tab = 0 }, Modifier.weight(1f))
                TxChip("تقویم خبری", tab == 1, { tab = 1 }, Modifier.weight(1f))
            }
        }
        Text("امروز: ${jalaliText(today)}", color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (tab == 0) {
            Reveal(1) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBox(Icons.Rounded.KeyboardArrowRight, "ماه قبل") { offset -= 1 }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(jalaliMonths[toJalali(ym.year, ym.monthValue, 15).second - 1] + " " + fa(toJalali(ym.year, ym.monthValue, 15).first), color = Tx.Text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                Text(ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.year, color = Tx.Muted, fontSize = 11.sp)
                            }
                            IconBox(Icons.Rounded.KeyboardArrowLeft, "ماه بعد") { offset += 1 }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            listOf("شنبه", "یک", "دوش", "سه", "چهار", "پنج", "جمعه").forEachIndexed { i, w ->
                                Text(w, color = if (i < 2) Tx.Muted.copy(alpha = 0.6f) else Tx.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                        val first = ym.atDay(1)
                        val lead = Analytics.column(first.toEpochDay())
                        val rows = (lead + ym.lengthOfMonth() + 6) / 7
                        for (r in 0 until rows) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0..6) {
                                    val day = r * 7 + c - lead + 1
                                    if (day in 1..ym.lengthOfMonth()) {
                                        val date = ym.atDay(day)
                                        DayCell(
                                            day = day,
                                            isToday = date == today,
                                            holiday = settings.weekendHoliday && c < 2,
                                            pnl = dayPnl[date.toEpochDay()],
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Reveal(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "High", "USD", "EUR", "GBP").forEachIndexed { i, f -> TxChip(f, filter == i, { filter = i }) }
                }
            }
            Reveal(2) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        SectionTitle("اخبار مهم امروز", "ساعت‌ها به وقت تهران")
                        val minImpact = when (settings.newsImpact) {
                            1 -> 1
                            2 -> 2
                            else -> 0
                        }
                        val list = sampleNews.filter { e ->
                            e.impact >= minImpact && when (filter) {
                                1 -> e.impact == 2
                                2 -> e.cur == "USD"
                                3 -> e.cur == "EUR"
                                4 -> e.cur == "GBP"
                                else -> true
                            }
                        }
                        list.forEach { e ->
                            val col = when (e.impact) {
                                2 -> Tx.Red
                                1 -> Tx.Gold
                                else -> Tx.Muted
                            }
                            Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(col))
                                Text(e.time, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(54.dp).padding(start = 10.dp))
                                Box(Modifier.width(44.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.08f)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                    Text(e.cur, color = Tx.Text, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(e.title, color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(listOf("Low", "Medium", "High")[e.impact] + " impact", color = col, fontSize = 11.sp)
                                }
                                val on = e.id in remind
                                Icon(
                                    if (on) Icons.Rounded.Notifications else Icons.Rounded.NotificationsNone,
                                    "یادآور",
                                    tint = if (on) Tx.Gold else Tx.Muted,
                                    modifier = Modifier.size(24.dp).clickable { remind = if (on) remind - e.id else remind + e.id },
                                )
                            }
                        }
                        if (list.isEmpty()) Text("خبری با این فیلتر نیست", color = Tx.Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
            Text("داده‌ها نمونه است؛ منبع نهایی تقویم خبری ForexFactory خواهد بود.", color = Tx.Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DayCell(day: Int, isToday: Boolean, holiday: Boolean, pnl: Double?, modifier: Modifier) {
    val shape = RoundedCornerShape(11.dp)
    val base = modifier.height(42.dp).clip(shape)
    val styled = when {
        isToday -> base.background(Brush.verticalGradient(listOf(Color(0xFFFFD978), Tx.GoldDeep)))
        holiday -> base.border(1.dp, Color.White.copy(alpha = 0.10f), shape)
        else -> base.background(Color.White.copy(alpha = 0.04f))
    }
    Column(styled, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(
            day.toString(),
            color = when {
                isToday -> Tx.Ink
                holiday -> Tx.Muted.copy(alpha = 0.6f)
                else -> Tx.Text
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (pnl == null) Color.Transparent else if (pnl >= 0) Tx.Green else Tx.Red))
    }
}
