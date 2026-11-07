# TRADENEX

دفترچه معاملات، آموزش و ساعت سشن‌های فارکس. **سیگنال نمی‌دهد و معامله انجام نمی‌دهد.**
Forex trade journal, learning hub and live session clock. It never trades and never gives signals.

## Stack

| Layer | Tech | Where |
|---|---|---|
| App + UI | Kotlin, Jetpack Compose (RTL, Persian first) | `app/` |
| Fast core | Rust (JNI): session maths, trade statistics | `rust/` |
| Animation kit | Dart / Flutter: globe, glow numbers | `flutter_fx/` |
| CI | GitHub Actions: `cargo test` + `assembleDebug` | `.github/workflows/ci.yml` |

## Screens

Home dashboard, sessions (spinning globe or 24h timeline, overlap map), add trade (lot size + profit/loss amount),
performance (equity candles daily/weekly/monthly/yearly + 5-week heatmap), strategies, education
(text / image / video and the 4 mixed combinations), calendar (trades + sample news), More menu with
`# هشتگ‌ها`, detailed settings.

## Build

1. Open in Android Studio (Koala+), let it sync. No Gradle wrapper is committed: use Studio's Gradle or run
   `gradle wrapper --gradle-version 8.9` once.
2. Optional, for the Rust core: `cargo install cargo-ndk`, set `ANDROID_NDK_HOME`, then `./rust/build-android.sh`.
   Without the `.so` the app uses the identical Kotlin fallback (`NativeCore.isNative` tells which one is active).
3. Dart animation kit: `cd flutter_fx && flutter run`. It is a standalone module for now and is not yet embedded in the
   Android app (the Compose globe in `Globe.kt` implements the same effect natively).

## Status / honest notes

- Written without a build machine: first thing to do is run the CI / a Gradle sync and fix any compile errors.
- Data is in memory with seeded sample trades; persistence (Room/DataStore) is not done.
- Sample news only; ForexFactory feed, QR sharing, real notifications (22:00 reminder), media attachments and the
  language switch (Persian/English) are UI-only or stubs.
- No Vazirmatn font bundled yet: add it under `res/font` and wire it in `Theme.kt`.
