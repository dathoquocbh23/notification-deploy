import 'package:flutter/material.dart';
import '../services/api_service.dart';

/// SD-3 · LỊCH SỬ THÔNG BÁO — đọc từ DB (nguồn chân lý), KHÔNG từ push.
/// Đủ cả những thông báo mà push bị rớt (token chết, mất mạng, tắt quyền).
class HistoryPage extends StatefulWidget {
  const HistoryPage({super.key});
  @override
  State<HistoryPage> createState() => _HistoryPageState();
}

class _HistoryPageState extends State<HistoryPage> {
  List<dynamic> _items = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final items = await ApiService.instance.notifications();
    if (mounted) setState(() { _items = items; _loading = false; });
  }

  IconData _icon(String? type) => switch (type) {
        'TRANSFER_IN' => Icons.south_west,
        'TRANSFER_OUT' => Icons.north_east,
        'PROMO' => Icons.card_giftcard,
        _ => Icons.notifications,
      };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Thông báo')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView.builder(
                itemCount: _items.length,
                itemBuilder: (_, i) {
                  final n = _items[i] as Map<String, dynamic>;
                  final unread = n['readAt'] == null;
                  return ListTile(
                    leading: Icon(_icon(n['type'] as String?)),
                    title: Text(n['title'] ?? '',
                        style: TextStyle(
                            fontWeight:
                                unread ? FontWeight.bold : FontWeight.normal)),
                    subtitle: Text(n['body'] ?? ''),
                    trailing: unread
                        ? const Icon(Icons.circle, size: 10, color: Colors.red)
                        : null,
                    onTap: () async {
                      await ApiService.instance.markRead(n['id'] as int);
                      _load();
                    },
                  );
                },
              ),
            ),
    );
  }
}
