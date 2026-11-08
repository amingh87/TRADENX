import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'fx_palette.dart';

/// Slow rising gold dust. Particle positions are a pure function of the index and the clock.
class ParticleField extends StatefulWidget {
  const ParticleField({super.key, this.count = 36});
  final int count;

  @override
  State<ParticleField> createState() => _ParticleFieldState();
}

class _ParticleFieldState extends State<ParticleField> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(seconds: 24))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pal = FxTheme.of(context);
    return IgnorePointer(
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, __) => CustomPaint(
          painter: _ParticlePainter(_c.value, widget.count, pal),
          size: Size.infinite,
        ),
      ),
    );
  }
}

class _ParticlePainter extends CustomPainter {
  _ParticlePainter(this.t, this.count, this.pal);
  final double t;
  final int count;
  final FxPalette pal;

  @override
  void paint(Canvas canvas, Size size) {
    final p = Paint();
    for (var i = 0; i < count; i++) {
      final seed = (i * 7919 % 1000) / 1000;
      final seed2 = (i * 104729 % 997) / 997;
      final phase = (t * (0.6 + seed2 * 0.9) + seed) % 1;
      final x = size.width * seed2 + math.sin(phase * 2 * math.pi + i) * 14;
      final y = size.height * (1.05 - phase * 1.1);
      final fade = math.sin(phase * math.pi).clamp(0.0, 1.0);
      final rad = 0.8 + seed * 1.8;
      p.color = pal.goldHi.withOpacity(0.55 * fade);
      canvas.drawCircle(Offset(x, y), rad, p);
      p.color = pal.gold.withOpacity(0.14 * fade);
      canvas.drawCircle(Offset(x, y), rad * 3.2, p);
    }
  }

  @override
  bool shouldRepaint(covariant _ParticlePainter old) => old.t != t || old.pal != pal;
}

/// Card with the theme's fill, a glowing border and (GOLD) a light sweep.
class GlassCardFx extends StatefulWidget {
  const GlassCardFx({super.key, required this.child, this.radius = 18, this.padding = const EdgeInsets.all(14)});
  final Widget child;
  final double radius;
  final EdgeInsets padding;

  @override
  State<GlassCardFx> createState() => _GlassCardFxState();
}

class _GlassCardFxState extends State<GlassCardFx> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 5200))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pal = FxTheme.of(context);
    final shape = BorderRadius.circular(widget.radius);
    return AnimatedBuilder(
      animation: _c,
      builder: (_, child) {
        final v = -0.4 + 1.8 * _c.value;
        return DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: shape,
            gradient: pal.luxe
                ? LinearGradient(colors: [pal.cardA, pal.cardB])
                : LinearGradient(colors: [Colors.white.withOpacity(0.09), Colors.white.withOpacity(0.02)]),
            border: Border.all(color: pal.gold.withOpacity(pal.luxe ? 0.5 : 0.38)),
            boxShadow: [BoxShadow(color: pal.gold.withOpacity(pal.luxe ? 0.3 : 0.2), blurRadius: 16)],
          ),
          child: ClipRRect(
            borderRadius: shape,
            child: Stack(
              children: [
                Padding(padding: widget.padding, child: child),
                if (pal.luxe)
                  Positioned.fill(
                    child: IgnorePointer(
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment(-1 + 2 * v - 0.5, -1),
                            end: Alignment(-1 + 2 * v + 0.5, 1),
                            colors: [Colors.transparent, pal.goldPale.withOpacity(0.10), Colors.transparent],
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
      child: widget.child,
    );
  }
}

/// Gold button with a breathing glow, a sweeping highlight and a press squish.
class ShimmerButton extends StatefulWidget {
  const ShimmerButton({super.key, required this.label, required this.onPressed});
  final String label;
  final VoidCallback onPressed;

  @override
  State<ShimmerButton> createState() => _ShimmerButtonState();
}

class _ShimmerButtonState extends State<ShimmerButton> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(milliseconds: 2400))..repeat();
  bool _down = false;

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pal = FxTheme.of(context);
    return GestureDetector(
      onTapDown: (_) => setState(() => _down = true),
      onTapCancel: () => setState(() => _down = false),
      onTapUp: (_) {
        setState(() => _down = false);
        widget.onPressed();
      },
      child: AnimatedScale(
        scale: _down ? 0.96 : 1,
        duration: const Duration(milliseconds: 120),
        child: AnimatedBuilder(
          animation: _c,
          builder: (_, __) {
            final glow = 0.25 + 0.35 * (0.5 + 0.5 * math.sin(_c.value * 2 * math.pi));
            final sweep = -1.5 + 3 * _c.value;
            return Container(
              height: 54,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: pal.goldPale),
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [pal.goldHi, pal.goldDeep],
                ),
                boxShadow: [BoxShadow(color: pal.gold.withOpacity(glow), blurRadius: 24)],
              ),
              child: ShaderMask(
                blendMode: BlendMode.srcATop,
                shaderCallback: (r) => LinearGradient(
                  begin: Alignment(sweep - 0.4, -0.3),
                  end: Alignment(sweep + 0.4, 0.3),
                  colors: [pal.ink, Colors.white, pal.ink],
                ).createShader(r),
                child: Text(
                  widget.label,
                  style: TextStyle(color: pal.ink, fontSize: 16, fontWeight: FontWeight.w800),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

/// Equity candles that grow from the baseline one after another.
class CandleGrow extends StatelessWidget {
  const CandleGrow({super.key, required this.candles, this.height = 140});

  /// Each entry is [open, high, low, close].
  final List<List<double>> candles;
  final double height;

  @override
  Widget build(BuildContext context) {
    final pal = FxTheme.of(context);
    return SizedBox(
      height: height,
      width: double.infinity,
      child: TweenAnimationBuilder<double>(
        key: ValueKey(candles.length),
        tween: Tween(begin: 0, end: 1),
        duration: Duration(milliseconds: 500 + candles.length * 60),
        curve: Curves.easeOutCubic,
        builder: (_, v, __) => CustomPaint(painter: _CandlePainter(candles, v, pal)),
      ),
    );
  }
}

class _CandlePainter extends CustomPainter {
  _CandlePainter(this.c, this.progress, this.pal);
  final List<List<double>> c;
  final double progress;
  final FxPalette pal;

  @override
  void paint(Canvas canvas, Size size) {
    if (c.isEmpty) return;
    var lo = c.first[2], hi = c.first[1];
    for (final k in c) {
      lo = math.min(lo, k[2]);
      hi = math.max(hi, k[1]);
    }
    final span = math.max(hi - lo, 1e-9);
    double y(double v) => size.height - (v - lo) / span * (size.height - 8) - 4;
    final slot = size.width / c.length;
    final p = Paint();
    for (var i = 0; i < c.length; i++) {
      // each candle starts a little later than the previous one
      final local = ((progress * (c.length + 4) - i) / 4).clamp(0.0, 1.0);
      if (local <= 0) continue;
      final k = c[i];
      final up = k[3] >= k[0];
      p.color = up ? pal.green : pal.red;
      final mid = y((k[0] + k[3]) / 2);
      double g(double v) => mid + (y(v) - mid) * local;
      final x = slot * i + slot / 2;
      canvas.drawLine(Offset(x, g(k[1])), Offset(x, g(k[2])), p..strokeWidth = 1.4);
      final top = math.min(g(k[0]), g(k[3]));
      final h = math.max((g(k[0]) - g(k[3])).abs(), 1.5);
      canvas.drawRRect(RRect.fromRectAndRadius(Rect.fromLTWH(x - slot * 0.3, top, slot * 0.6, h), const Radius.circular(2)), p);
    }
  }

  @override
  bool shouldRepaint(covariant _CandlePainter old) => old.progress != progress || old.c != c || old.pal != pal;
}
