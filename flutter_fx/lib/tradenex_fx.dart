import 'dart:math' as math;
import 'package:flutter/material.dart';

/// Spinning dotted globe with pulsing city markers.
class GlobeFx extends StatefulWidget {
  const GlobeFx({super.key, this.openSessions = const {2}});

  /// Indexes of open sessions: 0 Sydney, 1 Tokyo, 2 London, 3 New York.
  final Set<int> openSessions;

  @override
  State<GlobeFx> createState() => _GlobeFxState();
}

class _GlobeFxState extends State<GlobeFx> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(seconds: 40))..repeat();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) => CustomPaint(
        painter: _GlobePainter(_c.value, widget.openSessions),
        child: const SizedBox.expand(),
      ),
    );
  }
}

class _City {
  const _City(this.lat, this.lon, this.color, this.index);
  final double lat, lon;
  final Color color;
  final int index;
}

const _cities = [
  _City(-33.9, 151.2, Color(0xFF4DA3FF), 0),
  _City(35.7, 139.7, Color(0xFFB58CFF), 1),
  _City(51.5, -0.1, Color(0xFFF7CF6B), 2),
  _City(40.7, -74.0, Color(0xFFFF6B7A), 3),
];

class _GlobePainter extends CustomPainter {
  _GlobePainter(this.t, this.open);
  final double t;
  final Set<int> open;

  static const double _tilt = 0.35;

  Offset? _project(double latDeg, double lonDeg, double r, Offset c) {
    final lat = latDeg * math.pi / 180;
    final lon = (lonDeg - t * 360 + 20) * math.pi / 180;
    final x = math.cos(lat) * math.sin(lon);
    final y0 = math.sin(lat);
    final z0 = math.cos(lat) * math.cos(lon);
    final y = y0 * math.cos(_tilt) - z0 * math.sin(_tilt);
    final z = y0 * math.sin(_tilt) + z0 * math.cos(_tilt);
    if (z <= 0) return null;
    return Offset(c.dx + x * r, c.dy - y * r);
  }

  @override
  void paint(Canvas canvas, Size size) {
    final r = size.shortestSide * 0.42;
    final c = size.center(Offset.zero);
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..shader = const RadialGradient(
          colors: [Color(0xFF1D5FBF), Color(0xFF0C2C66), Color(0xFF040C22)],
        ).createShader(Rect.fromCircle(center: c, radius: r)),
    );
    final grid = Paint()
      ..color = const Color(0x337FB8FF)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8;
    for (var lat = -60; lat <= 60; lat += 30) {
      final path = Path();
      var started = false;
      for (var lon = -180; lon <= 180; lon += 6) {
        final p = _project(lat.toDouble(), lon.toDouble(), r, c);
        if (p == null) {
          started = false;
          continue;
        }
        started ? path.lineTo(p.dx, p.dy) : path.moveTo(p.dx, p.dy);
        started = true;
      }
      canvas.drawPath(path, grid);
    }
    final dot = Paint()..color = const Color(0xAA7FE3D0);
    for (var lat = -60; lat <= 70; lat += 5) {
      for (var lon = -180; lon < 180; lon += 5) {
        if (!_isLand(lat.toDouble(), lon.toDouble())) continue;
        final p = _project(lat.toDouble(), lon.toDouble(), r, c);
        if (p != null) canvas.drawCircle(p, 1.3, dot);
      }
    }
    final pulse = (t * 22) % 1;
    for (final city in _cities) {
      final p = _project(city.lat, city.lon, r, c);
      if (p == null) continue;
      final isOpen = open.contains(city.index);
      if (isOpen) {
        canvas.drawCircle(
          p,
          6 + 16 * pulse,
          Paint()
            ..color = city.color.withOpacity(0.6 * (1 - pulse))
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.5,
        );
      }
      canvas.drawCircle(p, isOpen ? 5 : 3.5, Paint()..color = city.color.withOpacity(isOpen ? 1 : 0.6));
    }
    canvas.drawCircle(
      c,
      r + 3,
      Paint()
        ..color = const Color(0x554DA3FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 6),
    );
  }

  // Very coarse land boxes (lat/lon): enough for a stylised dotted globe.
  bool _isLand(double lat, double lon) {
    const boxes = [
      [15, 70, -165, -55], // North America
      [-55, 12, -80, -35], // South America
      [36, 70, -10, 40], // Europe
      [-35, 35, -17, 50], // Africa
      [10, 70, 40, 145], // Asia
      [-38, -12, 114, 153], // Australia
    ];
    for (final b in boxes) {
      if (lat >= b[0] && lat <= b[1] && lon >= b[2] && lon <= b[3]) return true;
    }
    return false;
  }

  @override
  bool shouldRepaint(covariant _GlobePainter old) => old.t != t || old.open != open;
}

/// Number that counts up to [value] with a glow.
class GlowNumber extends StatelessWidget {
  const GlowNumber({super.key, required this.value, this.suffix = '', this.color = const Color(0xFF2BE8A8)});
  final double value;
  final String suffix;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: value),
      duration: const Duration(milliseconds: 1200),
      curve: Curves.easeOutCubic,
      builder: (_, v, __) => Text(
        '${v.toStringAsFixed(2)}$suffix',
        style: TextStyle(
          fontSize: 44,
          fontWeight: FontWeight.w800,
          color: color,
          shadows: [Shadow(color: color.withOpacity(0.5), blurRadius: 22)],
        ),
      ),
    );
  }
}
