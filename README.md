# TRADENEX

دفترچه معاملات، آموزش و ساعت سشن‌های فارکس. **سیگنال نمی‌دهد و معامله انجام نمی‌دهد.**
Forex trade journal, learning hub and live session clock. It never trades and never gives signals.

## Stack

| Layer | Tech | Where |
|---|---|---|
| App + UI | Kotlin, Jetpack Compose (RTL, Persian first) | `app/` |
| Fast core | Rust (JNI): session maths, trade statistics, globe projection (about 4.4k dots per frame), equity candles | `rust/` |
| Animation kit | Dart / Flutter: themes, GOLD globe, particles, shimmer button, glass card, candle grow | `flutter_fx/` |
| CI | GitHub Actions: `cargo test` + `assembleDebug` | `.github/workflows/ci.yml` |

## Screens

Home dashboard, sessions (spinning globe or 24h timeline, overlap map), add trade (lot size + profit/loss amount),
performance (equity candles daily/weekly/monthly/yearly + 5-week heatmap), strategies, education
(text / image / video and the 4 mixed combinations), calendar (trades + sample news), More menu with
`# هشتگ‌ها`, detailed settings.

## Themes

Settings > تم‌ها (also More > تم‌ها) switches between two themes with a 650 ms colour morph:

- **Default**: the original glass look with the blue ocean globe.
- **GOLD**: warm black and gold, gradient cards with a light sweep, gold FAB, and the dotted gold globe
  (day/night terminator, city lights, glowing session regions, pulse rings, trade arcs, orbiting satellite).

Also in Settings: accent colour (gold / emerald / violet), particles, shimmer, text size, card corner radius,
globe options, clear trades and reset (both with a confirmation). Settings are saved in SharedPreferences.

The globe's land dots live in `app/src/main/assets/world_dots.bin` (regenerate with `node tools/gen_world.mjs`);
Rust, Kotlin and Dart all read the same file.

## Build

1. Open in Android Studio (Koala+), let it sync. No Gradle wrapper is committed: use Studio's Gradle or run
   `gradle wrapper --gradle-version 8.9` once.
2. Rust tests without the `jni` crate: `cargo test --manifest-path rust/Cargo.toml --no-default-features`.
   Optional, for the Rust core: `cargo install cargo-ndk`, set `ANDROID_NDK_HOME`, then `./rust/build-android.sh`.
   Without the `.so` the app uses the identical Kotlin fallback (`NativeCore.isNative` tells which one is active).
3. Dart animation kit (has a Default/GOLD switcher demo): `cd flutter_fx && flutter run`. It is a standalone module for now and is not yet embedded in the
   Android app (the Compose globe in `Globe.kt` implements the same effect natively).

## Status / honest notes

- Written without a build machine: first thing to do is run the CI / a Gradle sync and fix any compile errors.
- Settings persist; trades are still in memory with seeded samples (Room/DataStore not done).
- Sample news only; ForexFactory feed, QR sharing, real notifications (22:00 reminder), media attachments and the
  language switch (Persian/English) are UI-only or stubs.
- No Vazirmatn font bundled yet: add it under `res/font` and wire it in `Theme.kt`.
