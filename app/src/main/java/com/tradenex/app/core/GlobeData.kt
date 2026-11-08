package com.tradenex.app.core

import android.content.Context

/** Loads `assets/world_dots.bin` into the engine (Rust when available) exactly once. */
object GlobeData {
    @Volatile
    var dotCount: Int = 0
        private set

    val ready: Boolean get() = dotCount > 0

    @Synchronized
    fun ensure(context: Context) {
        if (dotCount > 0) return
        dotCount = try {
            context.applicationContext.assets.open("world_dots.bin").use { NativeCore.globeLoad(it.readBytes()) }
        } catch (_: Throwable) {
            0
        }
    }
}
