import 'dart:io';
import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../services/push_service.dart';
import 'home_page.dart';

/// SD-1 · Đăng nhập = thời điểm GẮN TOKEN VÀO USER.
class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _email = TextEditingController();
  final _name = TextEditingController();
  bool _loading = false;

  Future<void> _login() async {
    final token = PushService.instance.token;
    if (token == null) return;
    setState(() => _loading = true);
    try {
      await ApiService.instance.login(
        email: _email.text.trim(),
        displayName: _name.text.trim(),
        fcmToken: token, // <-- token đi cùng login -> INSERT user_tokens
        deviceInfo: Platform.operatingSystem,
      );
      if (mounted) {
        Navigator.pushReplacement(
            context, MaterialPageRoute(builder: (_) => const HomePage()));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.account_balance_wallet, size: 64),
            const SizedBox(height: 8),
            const Text('Ví Demo', style: TextStyle(fontSize: 24)),
            const SizedBox(height: 24),
            TextField(
                controller: _email,
                decoration: const InputDecoration(labelText: 'Email')),
            const SizedBox(height: 12),
            TextField(
                controller: _name,
                decoration: const InputDecoration(labelText: 'Tên hiển thị')),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _loading ? null : _login,
              child: Text(_loading ? 'Đang đăng nhập...' : 'Đăng nhập'),
            ),
            const SizedBox(height: 12),
            const Text('Đăng nhập = gửi FCM token lên server\n(INSERT user_tokens)',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}
