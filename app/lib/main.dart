import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';

import 'services/local_notification_service.dart';
import 'services/push_service.dart';
import 'pages/login_page.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Bọc từng init: 1 service hỏng (vd FCM getToken trả SERVICE_NOT_AVAILABLE)
  // KHÔNG được làm sập app — UI vẫn phải vẽ. Lỗi chỉ log lại để chẩn đoán.
  try {
    await Firebase.initializeApp(); // cần firebase_options qua flutterfire configure
  } catch (e, st) {
    debugPrint('Firebase.initializeApp lỗi: $e\n$st');
  }
  try {
    await LocalNotificationService.instance.init();
  } catch (e, st) {
    debugPrint('LocalNotificationService.init lỗi: $e\n$st');
  }
  try {
    await PushService.instance.init();
  } catch (e, st) {
    debugPrint('PushService.init lỗi: $e\n$st');
  }
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
