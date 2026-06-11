import 'package:flutter/material.dart';
import '../services/api_service.dart';

/// SD-2 bước 2 · Nhập OTP (từ email) -> verify -> SỰ KIỆN thành công
/// -> backend fan-out: push cho mình (foreground bridge) + email biên lai
/// + push cho người nhận (SD-3 nếu máy kia tắt app).
class OtpPage extends StatefulWidget {
  final int transferId;
  const OtpPage({super.key, required this.transferId});
  @override
  State<OtpPage> createState() => _OtpPageState();
}

class _OtpPageState extends State<OtpPage> {
  final _otp = TextEditingController();
  String? _error;
  bool _loading = false;

  Future<void> _verify() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await ApiService.instance
          .verifyTransfer(widget.transferId, _otp.text.trim());
      if (!mounted) return;
      if (res['status'] == 'SUCCESS') {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Giao dịch thành công — chờ thông báo đẩy về!')));
      } else {
        setState(() => _error = res['error']?.toString() ?? 'Lỗi không rõ');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Xác thực OTP')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(children: [
          const Text('Mã OTP đã được gửi về email của bạn'),
          const SizedBox(height: 16),
          TextField(
            controller: _otp,
            keyboardType: TextInputType.number,
            maxLength: 6,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 24, letterSpacing: 8),
            decoration: InputDecoration(
                labelText: 'Nhập 6 số', errorText: _error),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _loading ? null : _verify,
            child: Text(_loading ? 'Đang xác thực...' : 'Xác nhận'),
          ),
        ]),
      ),
    );
  }
}
