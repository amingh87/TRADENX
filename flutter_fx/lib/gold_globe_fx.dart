import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;

import 'fx_palette.dart';

/// Land dots from `assets/world_dots.bin` (same file the Android app and the Rust core use).
/// Format: "TXW1", u32 count, then count x (i16 lon*100, i16 lat*100, u8 tag, u8 lit), little endian.
class WorldDots {
  WorldDots._(this.x, this.y, this.z, this.tag, this.lit);
  final Float32List x, y, z;
  final Uint8List tag, lit;
  int get length => x.length;

  static Future<WorldDots> load([String asset = 'assets/world_dots.bin']) async {
    final data = await rootBundle.load(asset);
    return parse(data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes));
  }

  static WorldDots parse(Uint8List b) {
    final bd = ByteData.sublistView(b);
    if (b.length < 8 || String.fromCharCodes(b.sublist(0, 4)) != 'TXW1') {
      throw const FormatException('bad world_dots magic');
    }
    final n = bd.getUint32(4, Endian.little);
    if (b.length < 8 + n * 6) throw const FormatException('world_dots truncated');
    final x = Float32List(n), y = Float32List(n), z = Float32List(n);
    final tag = Uint8List(n), lit = Uint8List(n);
    for (var i = 0; i < n; i++) {
      final o = 8 + i * 6;
      final lo = bd.getInt16(o, Endian.little) / 100 * math.pi / 180;
      final la = bd.getInt16(o + 2, Endian.little) / 100 * math.pi / 180;
      x[i] = math.cos(la) * math.sin(lo);
      y[i] = math.sin(la);
      z[i] = math.cos(la) * math.cos(lo);
      tag[i] = b[o + 4];
      lit[i] = b[o + 5];
    }
    return WorldDots._(x, y, z, tag, lit);
  }
}

class _Hub {
  const _Hub(this.name, this.lat, this.lon, this.session);
  final String name;
  final double lat, lon;
  final int session;
}

const _hubs = [
  _Hub('Sydney', -33.9, 151.2, 0),
  _Hub('Tokyo', 35.7, 139.7, 1),
  _Hub('London', 51.5, -0.1, 2),
  _Hub('New York', 40.7, -74.0, 3),
];

/// The GOLD globe in Flutter: land dots, day/night, city lights, pulses, arcs and a satellite.
class GoldGlobeFx extends StatefulWidget {
  const GoldGlobeFx({super.key, this.dots, this.openSessions = const {2}, this.utcMinute, this.night = true});

  /// Pass a preloaded [WorldDots]; when null the widget loads the asset itself.
  final WorldDots? dots;
  final Set<int> openSessions;

  /// Minute of the UTC day that places the sun. Defaults to now.
  final int? utcMinute;
  final bool night;

  @override
  State<GoldGlobeFx> createState() => _GoldGlobeFxState();
}

class _GoldGlobeFxState extends State<GoldGlobeFx> with SingleTickerProviderStateMixin {
  late final AnimationController _c =
      AnimationController(vsync: this, duration: const Duration(seconds: 60))..repeat();
  WorldDots? _dots;

  @override
  void initState() {
    super.initState();
    _dots = widget.dots;
    if (_dots == null) {
      WorldDots.load().then((d) {
        if (mounted) setState(() => _dots = d);
      }).catchError((_) {});
    }
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final pal = FxTheme.of(context);
    final now = DateTime.now().toUtc();
    final minute = widget.utcMinute ?? now.hour * 60 + now.minute;
    return AspectRatio(
      aspectRatio: 1,
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, __) => CustomPaint(
          painter: _GoldGlobePainter(_c.value, _dots, widget.openSessions, minute, widget.night, pal),
        ),
      ),
    );
  }
}

class _GoldGlobePainter extends CustomPainter {
  _GoldGlobePainter(this.t, this.dots, this.open, this.minute, this.night, this.pal);
  final double t;
  final WorldDots? dots;
  final Set<int> open;
  final int minute;
  final bool night;
  final FxPalette pal;

  static List<double> _unit(double latDeg, double lonDeg) {
    final la = latDeg * math.pi / 180, lo = lonDeg * math.pi / 180;
    return [math.cos(la) * math.sin(lo), math.sin(la), math.cos(la) * math.cos(lo)];
  }

  // (lon0, lat0) faces the viewer: same maths as rust/src/globe.rs
  static List<double> _rot(List<double> v, double lon0, double lat0, [double k = 1]) {
    final cl = math.cos(lon0 * math.pi / 180), sl = math.sin(lon0 * math.pi / 180);
    final cp = math.cos(lat0 * math.pi / 180), sp = math.sin(lat0 * math.pi / 180);
    final x1 = v[0] * cl - v[2] * sl;
    final z1 = v[0] * sl + v[2] * cl;
    final y2 = v[1] * cp - z1 * sp;
    final z2 = v[1] * sp + z1 * cp;
    return [x1 * k, y2 * k, z2 * k];
  }

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.shortestSide * 0.40;
    final lon0 = 20 + t * 360;
    final lat0 = 22 + 6 * math.sin(t * math.pi * 12);
    Offset pt(List<double> p) => Offset(c.dx + p[0] * r, c.dy - p[1] * r);

    // atmosphere + body
    canvas.drawCircle(
      c,
      r * 1.24,
      Paint()
        ..shader = RadialGradient(colors: [Colors.transparent, pal.gold.withOpacity(0.4), Colors.transparent])
            .createShader(Rect.fromCircle(center: c, radius: r * 1.24)),
    );
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..shader = RadialGradient(
          center: const Alignment(-0.35, -0.35),
          radius: 1.6,
          colors: [pal.goldDeep.withOpacity(0.55), pal.surface, pal.bg],
        ).createShader(Rect.fromCircle(center: c, radius: r)),
    );

    // graticule
    final grat = Paint()
      ..color = pal.gold.withOpacity(0.13)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.7;
    for (var lat = -60; lat <= 60; lat += 30) {
      final path = Path();
      var started = false;
      for (var lon = -180; lon <= 180; lon += 6) {
        final p = _rot(_unit(lat.toDouble(), lon.toDouble()), lon0, lat0);
        if (p[2] <= 0) {
          started = false;
          continue;
        }
        final o = pt(p);
        started ? path.lineTo(o.dx, o.dy) : path.moveTo(o.dx, o.dy);
        started = true;
      }
      canvas.drawPath(path, grat);
    }

    // land dots
    final d = dots;
    if (d != null) {
      final sun = _unit(-15.5, (12 - minute / 60) * 15);
      final base = size.shortestSide / 280;
      final paint = Paint();
      final cl = math.cos(lon0 * math.pi / 180), sl = math.sin(lon0 * math.pi / 180);
      final cp = math.cos(lat0 * math.pi / 180), sp = math.sin(lat0 * math.pi / 180);
      for (var i = 0; i < d.length; i++) {
        final x = d.x[i], y = d.y[i], z = d.z[i];
        final x1 = x * cl - z * sl;
        final z1 = x * sl + z * cl;
        final y2 = y * cp - z1 * sp;
        final z2 = y * sp + z1 * cp;
        if (z2 <= 0.02) continue;
        final day = night ? ((0.5 + (x * sun[0] + y * sun[1] + z * sun[2]) * 3).clamp(0.0, 1.0)) : 1.0;
        final shade = 0.35 + 0.65 * z2;
        final tag = d.tag[i];
        final lit = d.lit[i] / 255;
        final o = Offset(c.dx + x1 * r, c.dy - y2 * r);
        if (tag >= 1 && tag <= 4) {
          final on = open.contains(tag - 1);
          paint.color = pal.sessions[tag - 1].withOpacity(((on ? 0.95 : 0.45) * shade * (0.5 + 0.5 * day)).clamp(0.0, 1.0));
          canvas.drawCircle(o, base * (on ? 1.9 : 1.4), paint);
        } else if (lit > 0.25 && day < 0.45) {
          paint.color = pal.goldPale.withOpacity(((0.45 + 0.55 * lit) * shade).clamp(0.0, 1.0));
          canvas.drawCircle(o, base * 1.7, paint);
        } else {
          paint.color = pal.gold.withOpacity(((0.20 + 0.70 * day) * shade).clamp(0.0, 1.0));
          canvas.drawCircle(o, base * 1.5, paint);
        }
      }
    }

    // hubs
    final pulse = (t * 30) % 1;
    for (final h in _hubs) {
      final p = _rot(_unit(h.lat, h.lon), lon0, lat0);
      if (p[2] <= 0.05) continue;
      final o = pt(p);
      final col = pal.sessions[h.session];
      final isOpen = open.contains(h.session);
      if (isOpen) {
        canvas.drawCircle(
          o,
          6 + 20 * pulse,
          Paint()
            ..color = col.withOpacity(0.55 * (1 - pulse))
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.5,
        );
      }
      canvas.drawCircle(o, isOpen ? 5.2 : 3.6, Paint()..color = Colors.white);
      canvas.drawCircle(o, isOpen ? 3.8 : 2.6, Paint()..color = col);
      final tp = TextPainter(
        text: TextSpan(text: h.name, style: TextStyle(color: isOpen ? pal.text : pal.muted, fontSize: 9)),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, Offset(o.dx - tp.width / 2, o.dy + 7));
    }

    // rim light
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..color = pal.gold.withOpacity(0.55)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    // satellite
    final ang = t * 720 * math.pi / 180;
    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(-18 * math.pi / 180);
    final sat = Offset(math.cos(ang) * r * 1.28, math.sin(ang) * r * 0.34);
    canvas.drawOval(
      Rect.fromCenter(center: Offset.zero, width: r * 2.56, height: r * 0.68),
      Paint()
        ..color = pal.gold.withOpacity(0.28)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.2,
    );
    canvas.drawCircle(sat, 8, Paint()..color = pal.gold.withOpacity(0.3));
    canvas.drawCircle(sat, 3.2, Paint()..color = pal.goldPale);
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _GoldGlobePainter old) =>
      old.t != t || old.dots != dots || old.open != open || old.minute != minute || old.pal != pal || old.night != night;
}
