package com.tradenex.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tradenex.app.core.NativeCore
import com.tradenex.app.core.TradeStats
import java.time.LocalDate

data class Trade(
    val id: Long,
    val symbol: String,
    val buy: Boolean,
    val lot: Double,
    val pnl: Double,
    val strategy: String,
    val tags: List<String>,
    val epochDay: Long,
    val emotion: String,
)

data class Strategy(val id: Long, val name: String, val rules: List<String>, val active: Boolean)

enum class ContentKind(val label: String) {
    TEXT("فقط متن"),
    IMAGE("فقط عکس"),
    VIDEO("فقط ویدیو"),
    TEXT_IMAGE("متن + عکس"),
    IMAGE_TEXT("عکس + متن"),
    TEXT_VIDEO("متن + ویدیو"),
    VIDEO_TEXT("ویدیو + متن"),
}

data class Lesson(val id: Long, val title: String, val kind: ContentKind, val progress: Float)

data class AppSettings(
    val language: Int = 0, // 0 fa, 1 en (UI picker only for now)
    val showGlobe: Boolean = true,
    val tzMode: Int = 0, // 0 Tehran, 1 UTC, 2 device
    val reminderOn: Boolean = true,
    val reminderHour: Int = 22,
    val newsImpact: Int = 0, // 0 all, 1 medium+, 2 high
    val startBalance: Double = 10_000.0,
    val defaultLot: Double = 0.10,
    val riskPercent: Double = 1.0,
    val haptics: Boolean = true,
    val animations: Int = 1, // 0 low, 1 normal, 2 ultra
    val accent: Int = 0, // 0 gold, 1 emerald, 2 violet
    val weekendHoliday: Boolean = true,
    val sessionAlerts: Boolean = true,
    val newsAlerts: Boolean = true,
    val autoBackup: Boolean = false,
    val hideBalance: Boolean = false,
    val theme: Int = 0, // 0 default, 1 GOLD (see AppTheme)
    val textScale: Int = 1, // 0 small, 1 normal, 2 large
    val cardRadius: Int = 1, // 0 sharp, 1 normal, 2 round
    val particles: Boolean = true,
    val shimmer: Boolean = true,
    val globeNight: Boolean = true, // day/night shading on the Gold globe
    val globeCities: Boolean = true, // city lights + session labels on the Gold globe
)

object AppState {
    val trades = mutableStateListOf<Trade>().apply { addAll(seedTrades()) }
    val strategies = mutableStateListOf(
        Strategy(1, "ICT", listOf("تایید ساختار بازار", "ریسک حداکثر ۱٪", "فقط سشن لندن/نیویورک"), true),
        Strategy(2, "Supply & Demand", listOf("ناحیه تازه", "تایید کندلی"), true),
        Strategy(3, "Breakout", listOf("شکست با حجم", "استاپ زیر کندل شکست"), false),
    )
    val hashtags = mutableStateListOf("#london", "#news", "#fomo", "#breakout", "#discipline", "#revenge")
    val lessons = mutableStateListOf(
        Lesson(1, "پرایس اکشن از صفر", ContentKind.VIDEO_TEXT, 0.65f),
        Lesson(2, "قوانین مدیریت ریسک", ContentKind.TEXT, 1f),
        Lesson(3, "الگوهای کندلی کلیدی", ContentKind.IMAGE_TEXT, 0.3f),
    )
    private var prefs: SharedPreferences? = null

    var settings by mutableStateOf(AppSettings())
        private set

    /** Load saved settings. Call once from MainActivity.onCreate. */
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("tradenex_settings", Context.MODE_PRIVATE)
        prefs = p
        val d = AppSettings()
        settings = AppSettings(
            language = p.getInt("language", d.language),
            showGlobe = p.getBoolean("showGlobe", d.showGlobe),
            tzMode = p.getInt("tzMode", d.tzMode),
            reminderOn = p.getBoolean("reminderOn", d.reminderOn),
            reminderHour = p.getInt("reminderHour", d.reminderHour),
            newsImpact = p.getInt("newsImpact", d.newsImpact),
            startBalance = p.getFloat("startBalance", d.startBalance.toFloat()).toDouble(),
            defaultLot = p.getFloat("defaultLot", d.defaultLot.toFloat()).toDouble(),
            riskPercent = p.getFloat("riskPercent", d.riskPercent.toFloat()).toDouble(),
            haptics = p.getBoolean("haptics", d.haptics),
            animations = p.getInt("animations", d.animations),
            accent = p.getInt("accent", d.accent),
            weekendHoliday = p.getBoolean("weekendHoliday", d.weekendHoliday),
            sessionAlerts = p.getBoolean("sessionAlerts", d.sessionAlerts),
            newsAlerts = p.getBoolean("newsAlerts", d.newsAlerts),
            autoBackup = p.getBoolean("autoBackup", d.autoBackup),
            hideBalance = p.getBoolean("hideBalance", d.hideBalance),
            theme = p.getInt("theme", d.theme),
            textScale = p.getInt("textScale", d.textScale),
            cardRadius = p.getInt("cardRadius", d.cardRadius),
            particles = p.getBoolean("particles", d.particles),
            shimmer = p.getBoolean("shimmer", d.shimmer),
            globeNight = p.getBoolean("globeNight", d.globeNight),
            globeCities = p.getBoolean("globeCities", d.globeCities),
        )
    }

    /** Apply a change and persist it. */
    fun update(change: (AppSettings) -> AppSettings) {
        val s = change(settings)
        settings = s
        prefs?.edit()
            ?.putInt("language", s.language)
            ?.putBoolean("showGlobe", s.showGlobe)
            ?.putInt("tzMode", s.tzMode)
            ?.putBoolean("reminderOn", s.reminderOn)
            ?.putInt("reminderHour", s.reminderHour)
            ?.putInt("newsImpact", s.newsImpact)
            ?.putFloat("startBalance", s.startBalance.toFloat())
            ?.putFloat("defaultLot", s.defaultLot.toFloat())
            ?.putFloat("riskPercent", s.riskPercent.toFloat())
            ?.putBoolean("haptics", s.haptics)
            ?.putInt("animations", s.animations)
            ?.putInt("accent", s.accent)
            ?.putBoolean("weekendHoliday", s.weekendHoliday)
            ?.putBoolean("sessionAlerts", s.sessionAlerts)
            ?.putBoolean("newsAlerts", s.newsAlerts)
            ?.putBoolean("autoBackup", s.autoBackup)
            ?.putBoolean("hideBalance", s.hideBalance)
            ?.putInt("theme", s.theme)
            ?.putInt("textScale", s.textScale)
            ?.putInt("cardRadius", s.cardRadius)
            ?.putBoolean("particles", s.particles)
            ?.putBoolean("shimmer", s.shimmer)
            ?.putBoolean("globeNight", s.globeNight)
            ?.putBoolean("globeCities", s.globeCities)
            ?.apply()
    }

    fun clearTrades() {
        trades.clear()
    }

    fun stats(): TradeStats = NativeCore.stats(trades.map { it.pnl }.toDoubleArray())

    fun addTrade(t: Trade) {
        trades.add(t)
    }

    fun today(): Long = LocalDate.now().toEpochDay()

    private fun seedTrades(): List<Trade> {
        var s = 42L
        fun rnd(): Double {
            s = (s * 16807L) % 2147483647L
            return s / 2147483647.0
        }
        val syms = listOf("XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "BTCUSD")
        val strats = listOf("ICT", "Supply & Demand", "Breakout")
        val tags = listOf("#london", "#news", "#breakout", "#discipline")
        val emos = listOf("آرام", "ترس", "طمع", "خسته")
        val lots = listOf(0.1, 0.2, 0.5, 1.0)
        val out = ArrayList<Trade>()
        val today = LocalDate.now().toEpochDay()
        var id = 1L
        for (d in (today - 700)..today) {
            val dow = ((d + 5) % 7).toInt() // 0 Sat, 1 Sun, 2 Mon ... 6 Fri
            if (dow < 2) continue
            val n = (rnd() * 3).toInt()
            repeat(n) {
                val win = rnd() < 0.62
                val amount = 30 + rnd() * 220
                out.add(
                    Trade(
                        id = id++,
                        symbol = syms[(rnd() * syms.size).toInt()],
                        buy = rnd() < 0.55,
                        lot = lots[(rnd() * lots.size).toInt()],
                        pnl = if (win) amount else -amount * 0.8,
                        strategy = strats[(rnd() * strats.size).toInt()],
                        tags = listOf(tags[(rnd() * tags.size).toInt()]),
                        epochDay = d,
                        emotion = emos[(rnd() * emos.size).toInt()],
                    ),
                )
            }
        }
        return out
    }
}
