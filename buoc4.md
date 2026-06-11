## App chỉ là "tai nghe" — 3 việc
App Flutter KHÔNG chứa nghiệp vụ; nó chỉ là phía NHẬN của pipeline, làm đúng 3 việc:
- Lấy TOKEN: xin quyền thông báo + FirebaseMessaging.getToken() = "địa chỉ" của máy này, gửi lên server lúc login (INSERT user_tokens).
- LẮNG NGHE: đăng ký các handler nhận message (onMessage / onMessageOpenedApp / getInitialMessage) + subscribe topic 'all'.
- XỬ LÝ THEO TRẠNG THÁI app: foreground thì bắc cầu local notification; background/terminated thì để hệ điều hành tự hiện banner.
Mọi quyết định "gửi cho ai, ghi DB, dọn token chết" nằm ở backend — đổi app không ảnh hưởng điều phối.

## Nghiệm thu trên máy thật (đạt)
- Build APK release thành công, mở được trên máy thật (sau khi sửa trắng màn).
- Login -> user_tokens xuất hiện TOKEN THẬT dài ~142 ký tự (khác hẳn FAKE_TOKEN của test curl).
- Bắn transfer qua curl -> backend FCM OK (có msgId) -> máy nhận banner "Biến động số dư +250.000đ".

## Các lỗi gặp trong quá trình thực hiện

BUG: flutter analyze 4 errors — "The named parameter 'settings' is required" / "Too many positional arguments" tại local_notification_service.dart (initialize & show)
Bối cảnh: chạy flutter analyze trước khi build, dính 4 lỗi ở local_notification_service.dart.
Nguyên nhân gốc: code viết theo API cũ flutter_local_notifications (v17/18) nhưng pubspec ghim ^21; v21 đổi initialize/show sang named params.
Cách sửa: chuyển 4 call-site sang named params (initialize(settings: ...), show(id: ..., title: ..., body: ..., notificationDetails: ...)); flutter analyze về 0 lỗi.

BUG: New-NetFirewallRule "Access is denied" (Errno: quyền)
Bối cảnh: mở cổng 8080 cho điện thoại gọi backend qua LAN, chạy lệnh trong PowerShell thường.
Nguyên nhân gốc: tạo rule firewall là sửa cấu hình hệ thống -> bắt buộc quyền Administrator; cửa sổ PowerShell đang ở quyền thường.
Cách sửa: mở PowerShell Run as administrator, chạy New-NetFirewallRule -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow -Profile Any. Bài học: chỉ MỘT lệnh firewall này cần Admin; KHÔNG build bằng Admin vì có thể sinh file thuộc quyền admin gây lỗi quyền về sau.

BUG: firebase login mở nhầm trình duyệt Edge (không đúng tài khoản Google)
Bối cảnh: chạy firebase login để cấu hình FlutterFire; trình duyệt mặc định Windows là Edge trong khi tài khoản Google nằm ở Chrome.
Nguyên nhân gốc: firebase login mở URL OAuth bằng trình duyệt MẶC ĐỊNH của hệ điều hành, không phải trình duyệt đang đăng nhập Google.
Cách sửa: firebase login --no-localhost -> copy URL, mở thủ công bằng Chrome -> đăng nhập -> dán authorization code ngược lại terminal.

BUG: 'flutterfire' is not recognized as an internal or external command
Bối cảnh: chạy flutterfire configure ngay sau khi dart pub global activate flutterfire_cli.
Nguyên nhân gốc: lệnh activate cài binary vào %LOCALAPPDATA%\Pub\Cache\bin nhưng thư mục này chưa nằm trong PATH.
Cách sửa: thêm %LOCALAPPDATA%\Pub\Cache\bin vào PATH (phiên hiện tại bằng set PATH=...; vĩnh viễn bằng SetEnvironmentVariable mức User), mở terminal mới.

BUG: 'adb' is not recognized as an internal or external command
Bối cảnh: cần adb để cài APK + xem logcat trên máy thật.
Nguyên nhân gốc: cùng họ với lỗi flutterfire — Android SDK ở %LOCALAPPDATA%\Android\Sdk nhưng platform-tools chưa nằm trong PATH.
Cách sửa: thêm %LOCALAPPDATA%\Android\Sdk\platform-tools vào PATH (như trên). Nhận xét: 2 lỗi cùng pattern -> trên Windows, tool cài qua package manager thường KHÔNG tự thêm vào PATH, phải tự nối tay.

BUG: Device not authorized — adb devices báo "unauthorized"
Bối cảnh: cắm USB, flutter doctor/adb thấy thiết bị nhưng không dùng được.
Nguyên nhân gốc: máy chưa chấp nhận hộp thoại "Allow USB debugging" từ máy tính này -> adb chưa được uỷ quyền.
Cách sửa: trên điện thoại tick "Always allow from this computer" -> Allow; adb devices chuyển từ unauthorized sang device.

BUG: Màn hình trắng khi mở bản release — "E/flutter: Unhandled Exception: [firebase_messaging/unknown] java.io.IOException: ... SERVICE_NOT_AVAILABLE" tại PushService.init (push_service.dart:38) -> main (main.dart:12)
Bối cảnh: cài APK release lên máy thật, mở app chỉ thấy nền trắng, không vào được màn Login.
Nguyên nhân gốc: trong main(), dòng await PushService.init() gọi getToken() ném SERVICE_NOT_AVAILABLE (Firebase Installations Service không khả dụng vì mạng chặn Google); exception KHÔNG được bắt -> main() dừng TRƯỚC runApp() -> UI không bao giờ vẽ. (Phân biệt với triệu chứng: KHÔNG phải FirebaseApp chưa init — log có "FirebaseApp initialization successful"; KHÔNG phải R8/minify mất class; KHÔNG phải lỗi baseUrl.)
Cách sửa: bọc try-catch quanh từng init trong main.dart (Firebase.initializeApp / LocalNotificationService.init / PushService.init), log lỗi và LUÔN gọi runApp() -> UI hiện kể cả khi 1 service hỏng. Xác minh bằng adb logcat: hết "Unhandled Exception", màn Login hiện.

## Nhận xét chung
Phần lớn lỗi hôm nay là lỗi MÔI TRƯỜNG WINDOWS (quyền Administrator, PATH chưa có tool, trình duyệt mặc định sai tài khoản, USB chưa uỷ quyền) chứ KHÔNG phải lỗi logic của hệ thống. Đây đúng đặc thù giai đoạn nối app thật vào hạ tầng thật: code đã đúng từ các bước trước, việc khó nằm ở khâu dựng môi trường máy thật. Công cụ quyết định để THOÁT ĐOÁN MÒ và tìm đúng nguyên nhân trắng màn là adb logcat — đọc nguyên văn stacktrace thay vì suy diễn.
