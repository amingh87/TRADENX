import 'package:flutter/material.dart';
import 'tradenex_fx.dart';

void main() => runApp(const FxDemo());

/// Standalone preview of the Dart animation kit: `flutter run` inside flutter_fx/.
class FxDemo extends StatelessWidget {
  const FxDemo({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(useMaterial3: true),
      home: const Scaffold(
        backgroundColor: Color(0xFF07070A),
        body: SafeArea(
          child: Column(
            children: [
              SizedBox(height: 24),
              GlowNumber(value: 24.75, suffix: '%'),
              Expanded(child: GlobeFx(openSessions: {2})),
            ],
          ),
        ),
      ),
    );
  }
}
