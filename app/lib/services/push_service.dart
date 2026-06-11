import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import 'api_service.dart';
import 'local_notification_service.dart';

/// ============================================================
///  PHÍA NHẬN PUSH — app chỉ làm 3 việc:
///  (1) xin quyền + lấy TOKEN (địa chỉ của máy này)
///  (2) xử lý theo 3 TRẠNG THÁI app
///  (3) báo server khi token ĐỔI (vòng đời token — SD-1)
/// ============================================================

/// TERMINATED/BACKGROUND handler — BẮT BUỘC top-level + @pragma
/// (chạy ở isolate riêng khi app không sống; Dart cần entry-point
/// để hàm không bị tree-shake khỏi bản release).
@pragma('vm:entry-point')
Future<void> firebaseBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  debugPrint('[BG/Terminated] ${message.notification?.title}');
  // KHÔNG cần tự hiện gì — notification message do HỆ ĐIỀU HÀNH hiện (SD-3).
}

class PushService {
  PushService._();
  static final PushService instance = PushService._();

  final _fm = FirebaseMessaging.instance;
  String? token;

  /// Callback để UI điều hướng khi user tap notification.
  void Function()? onOpenHistory;

  Future<void> init() async {
    // (1) Quyền + token
    await _fm.requestPermission(alert: true, badge: true, sound: true);
    token = await _fm.getToken();
    debugPrint('=== FCM TOKEN (địa chỉ máy này) ===\n$token');

    // Topic 'all' — kênh broadcast khuyến mãi (1-nhiều).
    await _fm.subscribeToTopic('all');

    // (3) Token tự đổi -> báo server UPDATE (SD-1 phần refresh)
    _fm.onTokenRefresh.listen((newToken) {
      final old = token ?? '';
      token = newToken;
      ApiService.instance.refreshFcmToken(old, newToken);
    });

    // (2a) FOREGROUND — FCM không tự hiện -> bắc cầu local (SD-2)
    FirebaseMessaging.onMessage.listen((m) {
      final n = m.notification;
      if (n != null) {
        LocalNotificationService.instance.show(n.title ?? '', n.body ?? '');
      }
    });

    // (2b) BACKGROUND — user tap noti khi app chạy nền -> mở lịch sử
    FirebaseMessaging.onMessageOpenedApp.listen((_) => onOpenHistory?.call());

    // (2c) TERMINATED — app mở DO user tap noti lúc app tắt (SD-3)
    final initial = await _fm.getInitialMessage();
    if (initial != null) {
      // App vừa được OS khởi động từ banner -> vào thẳng lịch sử
      Future.delayed(const Duration(milliseconds: 300),
          () => onOpenHistory?.call());
    }

    FirebaseMessaging.onBackgroundMessage(firebaseBackgroundHandler);
  }
}
