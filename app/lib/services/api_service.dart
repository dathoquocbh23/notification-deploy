import 'dart:convert';
import 'package:http/http.dart' as http;

/// Gọi backend Spring Boot.
/// LƯU Ý KHI DEMO: điện thoại thật phải cùng Wi-Fi với laptop chạy backend,
/// và baseUrl là IP LAN của laptop (KHÔNG phải localhost) — hoặc trỏ thẳng
/// lên server đã deploy.
class ApiService {
  ApiService._();
  static final ApiService instance = ApiService._();

  // Backend deploy trên Railway (HTTPS, cổng 443) — đi qua internet nên KHÔNG
  // lệ thuộc LAN/WiFi/AP-isolation; đổi mạng 4G/WiFi gì cũng chạy, không cần build lại.
  // (Muốn chạy backend local thì đổi sang http://<IP-LAN>:8080 + bật lại usesCleartextTraffic.)
  static const String baseUrl = 'https://notification-deploy-production.up.railway.app';

  String? currentEmail; // user đang đăng nhập (auth giả lập)

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) async {
    final res = await http.post(Uri.parse('$baseUrl$path'),
        headers: {'Content-Type': 'application/json'}, body: jsonEncode(body));
    return jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> login({
    required String email,
    required String displayName,
    required String fcmToken,
    required String deviceInfo,
  }) async {
    final data = await _post('/auth/login', {
      'email': email,
      'displayName': displayName,
      'fcmToken': fcmToken,
      'deviceInfo': deviceInfo,
    });
    currentEmail = email;
    return data;
  }

  Future<void> logout(String fcmToken) async {
    await _post('/auth/logout', {'fcmToken': fcmToken});
    currentEmail = null;
  }

  Future<void> refreshFcmToken(String oldToken, String newToken) async {
    await http.put(Uri.parse('$baseUrl/auth/fcm-token'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'oldToken': oldToken, 'newToken': newToken}));
  }

  Future<Map<String, dynamic>> createTransfer(
          String toEmail, int amount) =>
      _post('/demo/transfers',
          {'fromEmail': currentEmail, 'toEmail': toEmail, 'amount': amount});

  Future<Map<String, dynamic>> verifyTransfer(int transferId, String otp) =>
      _post('/demo/transfers/$transferId/verify', {'otp': otp});

  Future<List<dynamic>> notifications() async {
    final res = await http.get(
        Uri.parse('$baseUrl/notifications?email=$currentEmail'));
    return jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
  }

  Future<void> markRead(int id) => _post('/notifications/$id/read', {});
}
