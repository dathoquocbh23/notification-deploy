import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';

import 'services/local_notification_service.dart';
import 'services/push_service.dart';
import 'pages/login_page.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(); // cần firebase_options qua flutterfire configure
  await LocalNotificationService.instance.init();
  await PushService.instance.init();
  runApp(const NotifyDemoApp());
}

class NotifyDemoApp extends StatelessWidget {
  const NotifyDemoApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Ví Demo',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(useMaterial3: true, colorSchemeSeed: const Color(0xFF1D3557)),
        home: const LoginPage(),
      );
}
