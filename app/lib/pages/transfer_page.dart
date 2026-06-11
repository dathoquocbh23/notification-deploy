import 'package:flutter/material.dart';
import '../services/api_service.dart';
import 'otp_page.dart';

/// SD-2 bước 1 · Khởi tạo chuyển tiền -> backend gửi OTP qua EMAIL.
class TransferPage extends StatefulWidget {
  const TransferPage({super.key});
  @override
  State<TransferPage> createState() => _TransferPageState();
}

class _TransferPageState extends State<TransferPage> {
  final _toEmail = TextEditingController();
  final _amount = TextEditingController(text: '100000');
  bool _loading = false;

  Future<void> _submit() async {
    setState(() => _loading = true);
    try {
      final res = await ApiService.instance.createTransfer(
          _toEmail.text.trim(), int.parse(_amount.text.trim()));
      if (mounted && res['transferId'] != null) {
        Navigator.pushReplacement(
            context,
            MaterialPageRoute(
                builder: (_) => OtpPage(transferId: res['transferId'] as int)));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Chuyển tiền')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(children: [
          TextField(
              controller: _toEmail,
              decoration:
                  const InputDecoration(labelText: 'Email người nhận')),
          const SizedBox(height: 12),
          TextField(
              controller: _amount,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Số tiền (đ)')),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _loading ? null : _submit,
            child: Text(_loading ? 'Đang xử lý...' : 'Tiếp tục'),
          ),
          const SizedBox(height: 12),
          const Text('Bấm Tiếp tục -> backend gửi mã OTP về email của bạn',
              style: TextStyle(fontSize: 12, color: Colors.grey)),
        ]),
      ),
    );
  }
}
