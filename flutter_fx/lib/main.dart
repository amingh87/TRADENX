import 'package:flutter/material.dart';

import 'tradenex_fx.dart';

void main() => runApp(const FxDemo());

/// Standalone preview of the Dart animation kit: `flutter run` inside flutter_fx/.
/// The switch at the top morphs between the Default and GOLD themes.
class FxDemo extends StatefulWidget {
  const FxDemo({super.key});

  @override
  State<FxDemo> createState() => _FxDemoState();
}

class _FxDemoState extends State<FxDemo> {
  int _theme = 1;

  static const _candles = <List<double>>[
    [10000, 10120, 9980, 10090],
    [10090, 10200, 10060, 10180],
    [10180, 10190, 10040, 10070],
    [10070, 10310, 10050, 10290],
    [10290, 10330, 10200, 10230],
    [10230, 10480, 10210, 10450],
    [10450, 10520, 10400, 10410],
    [10410, 10660, 10390, 10640],
  ];

  @override
  Widget build(BuildContext context) {
    return FxThemeSwitcher(
      target: FxPalette.all[_theme],
      builder: (context) {
        final pal = FxTheme.of(context);
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: ThemeData.dark(useMaterial3: true).copyWith(scaffoldBackgroundColor: pal.bg),
          home: Scaffold(
            body: Stack(
              children: [
                const Positioned.fill(child: ParticleField()),
                SafeArea(
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      SegmentedButton<int>(
                        segments: [
                          for (var i = 0; i < FxPalette.all.length; i++)
                            ButtonSegment(value: i, label: Text(FxPalette.all[i].name)),
                        ],
                        selected: {_theme},
                        onSelectionChanged: (s) => setState(() => _theme = s.first),
                      ),
                      const SizedBox(height: 16),
                      GlassCardFx(
                        child: Column(
                          children: [
                            Text('عملکرد ۳۰ روز اخیر', style: TextStyle(color: pal.text)),
                            GlowNumber(value: 24.75, suffix: '%', color: pal.green),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        height: 320,
                        child: _theme == 1
                            ? const GoldGlobeFx(openSessions: {2, 3})
                            : const GlobeFx(openSessions: {2}),
                      ),
                      GlassCardFx(child: CandleGrow(candles: _candles)),
                      const SizedBox(height: 16),
                      ShimmerButton(label: 'ثبت معامله جدید', onPressed: () {}),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
