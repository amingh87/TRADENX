import 'package:flutter/material.dart';

/// Colours of one theme. Mirrors `Palette` in the Android app (Theme.kt).
@immutable
class FxPalette {
  const FxPalette({
    required this.name,
    required this.bg,
    required this.gold,
    required this.goldHi,
    required this.goldDeep,
    required this.goldPale,
    required this.green,
    required this.red,
    required this.text,
    required this.muted,
    required this.ink,
    required this.surface,
    required this.cardA,
    required this.cardB,
    required this.sessions,
    required this.luxe,
  });

  final String name;
  final Color bg, gold, goldHi, goldDeep, goldPale, green, red, text, muted, ink, surface, cardA, cardB;

  /// Sydney, Tokyo, London, New York.
  final List<Color> sessions;
  final bool luxe;

  static const defaultTheme = FxPalette(
    name: 'Default',
    bg: Color(0xFF07070A),
    gold: Color(0xFFF7CF6B),
    goldHi: Color(0xFFFFD978),
    goldDeep: Color(0xFFC98E1E),
    goldPale: Color(0xFFFFE9A8),
    green: Color(0xFF2BE8A8),
    red: Color(0xFFFF6B7A),
    text: Color(0xFFF4EFE3),
    muted: Color(0xFFB4AE9F),
    ink: Color(0xFF1A1204),
    surface: Color(0xFF14141A),
    cardA: Color(0x17FFFFFF),
    cardB: Color(0x05FFFFFF),
    sessions: [Color(0xFF4DA3FF), Color(0xFFB58CFF), Color(0xFFF7CF6B), Color(0xFFFF6B7A)],
    luxe: false,
  );

  static const goldTheme = FxPalette(
    name: 'GOLD',
    bg: Color(0xFF070608),
    gold: Color(0xFFF2B53D),
    goldHi: Color(0xFFFFD884),
    goldDeep: Color(0xFFB9780F),
    goldPale: Color(0xFFFFE7A8),
    green: Color(0xFF1FD673),
    red: Color(0xFFFF3D4F),
    text: Color(0xFFF7EDD8),
    muted: Color(0xFFAB9E82),
    ink: Color(0xFF1A1004),
    surface: Color(0xFF17110A),
    cardA: Color(0xFF1A130B),
    cardB: Color(0xFF0D0A07),
    sessions: [Color(0xFF2F8CFF), Color(0xFF1FD673), Color(0xFFFFB31F), Color(0xFFFF3D4F)],
    luxe: true,
  );

  static const all = [defaultTheme, goldTheme];

  /// Linear blend, used for the animated theme switch.
  static FxPalette lerp(FxPalette a, FxPalette b, double t) {
    Color c(Color x, Color y) => Color.lerp(x, y, t)!;
    return FxPalette(
      name: t < 0.5 ? a.name : b.name,
      bg: c(a.bg, b.bg),
      gold: c(a.gold, b.gold),
      goldHi: c(a.goldHi, b.goldHi),
      goldDeep: c(a.goldDeep, b.goldDeep),
      goldPale: c(a.goldPale, b.goldPale),
      green: c(a.green, b.green),
      red: c(a.red, b.red),
      text: c(a.text, b.text),
      muted: c(a.muted, b.muted),
      ink: c(a.ink, b.ink),
      surface: c(a.surface, b.surface),
      cardA: c(a.cardA, b.cardA),
      cardB: c(a.cardB, b.cardB),
      sessions: [for (var i = 0; i < 4; i++) c(a.sessions[i], b.sessions[i])],
      luxe: t < 0.5 ? a.luxe : b.luxe,
    );
  }
}

/// Provides the active palette to the kit's widgets.
class FxTheme extends InheritedWidget {
  const FxTheme({super.key, required this.palette, required super.child});
  final FxPalette palette;

  static FxPalette of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<FxTheme>()?.palette ?? FxPalette.defaultTheme;

  @override
  bool updateShouldNotify(FxTheme old) => old.palette != palette;
}

/// Cross-fades palettes when [target] changes.
class FxThemeSwitcher extends StatelessWidget {
  const FxThemeSwitcher({super.key, required this.target, required this.builder});
  final FxPalette target;
  final Widget Function(BuildContext context) builder;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<FxPalette>(
      tween: _PaletteTween(end: target),
      duration: const Duration(milliseconds: 650),
      curve: Curves.easeInOutCubic,
      builder: (ctx, p, _) => FxTheme(palette: p, child: Builder(builder: builder)),
    );
  }
}

class _PaletteTween extends Tween<FxPalette> {
  _PaletteTween({required FxPalette end}) : super(begin: end, end: end);
  @override
  FxPalette lerp(double t) => FxPalette.lerp(begin!, end!, t);
}
