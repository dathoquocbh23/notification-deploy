import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// Cầu nối FOREGROUND: FCM không tự hiện banner khi app đang mở,
/// nên onMessage gọi sang đây để hiện bằng local notification (SD-2).
class LocalNotificationService {
  LocalNotificationService._();
  static final LocalNotificationService instance = LocalNotificationService._();

  final _plugin = FlutterLocalNotificationsPlugin();

  static const _channel = AndroidNotificationChannel(
    'banking_channel',
    'Thông báo giao dịch',
    description: 'Biến động số dư, OTP, khuyến mãi',
    importance: Importance.max,
  );

  Future<void> init() async {
    const settings = InitializationSettings(
      android: AndroidInitializationSettings('@mipmap/ic_launcher'),
      iOS: DarwinInitializationSettings(),
    );
    await _plugin.initialize(settings);
    await _plugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(_channel);
    await _plugin
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
  }

  Future<void> show(String title, String body) => _plugin.show(
        DateTime.now().millisecondsSinceEpoch ~/ 1000,
        title,
        body,
        NotificationDetails(
          android: AndroidNotificationDetails(
            _channel.id, _channel.name,
            channelDescription: _channel.description,
            importance: Importance.max, priority: Priority.high,
          ),
          iOS: const DarwinNotificationDetails(),
        ),
      );
}
