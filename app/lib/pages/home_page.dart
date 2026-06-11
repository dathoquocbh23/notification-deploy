import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../services/push_service.dart';
import 'transfer_page.dart';
import 'history_page.dart';
import 'login_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});
  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  @override
  void initState() {
    super.initState();
    // Tap notification (background/terminated) -> mở thẳng lịch sử
    PushService.instance.onOpenHistory = () {
      if (mounted) {
        Navigator.push(context,
            MaterialPageRoute(builder: (_) => const HistoryPage()));
      }
    };
  }

  Future<void> _logout() async {
    // SD-1 · Đăng xuất = XOÁ token khỏi server.
    // Không xoá -> máy này vẫn nhận biến động số dư của tài khoản cũ.
    final token = PushService.instance.token;
    if (token != null) await ApiService.instance.logout(token);
    if (mounted) {
      Navigator.pushAndRemoveUntil(context,
          MaterialPageRoute(builder: (_) => const LoginPage()), (_) => false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final email = ApiService.instance.currentEmail ?? '';
    return Scaffold(
      appBar: AppBar(
        title: Text(email),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications),
            onPressed: () => Navigator.push(context,
                MaterialPageRoute(builder: (_) => const HistoryPage())),
          ),
          IconButton(icon: const Icon(Icons.logout), onPressed: _logout),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(children: const [
                  Text('Số dư (giả lập)'),
                  SizedBox(height: 8),
                  Text('1.000.000đ',
                      style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold)),
                ]),
              ),
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              icon: const Icon(Icons.send),
              label: const Text('Chuyển tiền'),
              onPressed: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const TransferPage())),
            ),
            const Spacer(),
            SelectableText(
              'FCM token (demo):\n${PushService.instance.token ?? ""}',
              style: const TextStyle(fontSize: 10, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}
