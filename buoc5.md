## Mục tiêu
Đưa backend ra internet (deploy cloud) để app gọi qua domain HTTPS, hết lệ thuộc LAN/WiFi. Nền tảng chọn: Railway (free trial, build từ GitHub).

## Vì sao phải deploy (gốc rễ từ các lần trước)
Mọi cách nối LAN đều chết vì AP-isolation: WiFi chung cư và cả thiết bị 4G hosting đều cô lập client với nhau (điện thoại không thấy laptop dù cùng subnet, firewall đã mở). USB + adb reverse chạy được nhưng phải có dây. Giải pháp dứt điểm: backend có domain công khai -> app đi qua internet, đổi mạng gì cũng chạy, không build lại.

## Cấu hình Railway (làm trên dashboard)
- Service từ GitHub repo -> Settings -> Source -> Root Directory = backend (monorepo, chỉ build thư mục backend, không ôm cả cục).
- Variables: nạp DB_URL, DB_USER, DB_PASSWORD, FIREBASE_SA_JSON (dán toàn bộ service-account.json), BREVO_API_KEY, BREVO_SENDER. KHÔNG set PORT (Railway tự cấp).
- Settings -> Networking -> Generate Domain -> ra https://notification-deploy-production.up.railway.app.

## Thay đổi code để chạy cloud
- backend/Dockerfile: build Maven 2 stage (maven:3.9-eclipse-temurin-17 -> eclipse-temurin:17-jre chạy jar). Có Dockerfile thì Railway build xác định, không phụ thuộc Nixpacks tự đoán.
- application.yml: server.port = ${PORT:8080} -> khớp cổng động Railway cấp.
- FirebaseConfig: đọc service-account từ env FIREBASE_SA_JSON (fallback file local) -> FCM chạy trên cloud mà không cần commit file key.

## Các lỗi gặp khi deploy

BUG: Railway build "Failed to build an image" — deployment FAILED during build process (builder Nixpacks)
Bối cảnh: deploy lần đầu, Railway tự dùng Nixpacks build project Maven.
Nguyên nhân gốc: Nixpacks tự đoán cách build/run Java/Maven hay trượt (không xác định được start command / artifact).
Cách sửa: thêm backend/Dockerfile build 2 stage tường minh; set Root Directory = backend. Railway thấy Dockerfile thì dùng nó thay Nixpacks -> build thành công, service Online.

BUG: POST /demo/transfers TREO trên cloud (request không trả về, curl timeout) dù transfer đã lưu DB
Bối cảnh: app đã trỏ về domain cloud, bấm chuyển tiền thì kẹt; kiểm tra DB thấy transfer được lưu (status PENDING) nhưng HTTP response không bao giờ về.
Nguyên nhân gốc: DemoController.create() lưu transfer xong gọi emailService.sendOtp() qua Brevo SMTP relay cổng 2525; SERVER CLOUD (Railway) CHẶN cổng SMTP outbound -> lời gọi SMTP treo timeout -> chặn luôn request (vì @Async bị self-invocation bypass nên send() chạy đồng bộ). Verify cũng treo ở sendReceipt -> push người nhận không bao giờ chạy. Đây CHÍNH XÁC là lý do thiết kế gốc ghi "email qua Brevo HTTP API, không SMTP vì server chặn cổng SMTP".
Cách sửa: đổi EmailService sang Brevo HTTP API (RestClient gọi https://api.brevo.com/v3/smtp/email cổng 443 — không bị chặn), có try-catch nên KHÔNG bao giờ treo request kể cả khi key sai/thiếu. Bỏ spring-boot-starter-mail. Sau khi deploy lại: /demo/transfers trả về ngay, verify chạy thẳng tới push người nhận.

BUG: Email không gửi, Brevo Logs KHÔNG có dòng nào hôm nay (dù transfer trả về nhanh, không treo)
Bối cảnh: sau khi chuyển sang HTTP API, request hết treo nhưng OTP vẫn không về inbox; mở Brevo Logs chỉ thấy email SMTP cũ, không có dòng mới.
Nguyên nhân gốc: tài khoản Brevo bật "Authorized IPs" cho API keys (Security -> Authorized IPs: API keys = Activated) trong khi danh sách IP cho phép = 0 -> MỌI lời gọi API bị chặn ở tầng IP, trước cả khi ghi log. (SMTP keys lại Deactivated nên email SMTP hôm trước vẫn chạy — đó là lý do gây nhầm.)
Cách sửa: Brevo -> Settings -> Security -> Authorized IPs -> "Deactivate for API keys" (không whitelist IP Railway vì IP cloud động). Xác minh bằng cách gọi thẳng api.brevo.com từ laptop -> trả 201 + messageId => key/sender/IP đã ổn.

BUG: Gọi thẳng từ laptop 201 nhưng từ Railway vẫn không gửi — BREVO_API_KEY RỖNG trên instance
Bối cảnh: key gọi trực tiếp được (201) mà backend cloud vẫn im; nghi biến môi trường chưa vào.
Nguyên nhân gốc: biến BREVO_API_KEY đã tạo trên Railway nhưng CHƯA được apply vào bản deploy đang chạy (Railway "stage" thay đổi biến, chưa bấm Deploy) -> trong container biến rỗng -> Brevo trả 401 -> không log.
Cách sửa: thêm endpoint tạm /debug/brevo trả keyLen + brevoStatus -> đọc thấy keyLen=0 ("RỖNG trên instance"). Apply lại biến + redeploy -> /debug/brevo trả keyLen=89, keyPrefix=xkeysib-, brevoStatus=201. Gửi OTP luồng thật -> Brevo Logs hiện Delivered, email về inbox. Xoá endpoint debug sau khi xong.
Bài học: lỗi "không gửi được" trên cloud có NHIỀU LỚP (giao thức SMTP bị chặn -> whitelist IP của nhà cung cấp -> biến môi trường chưa nạp). Tách từng lớp bằng bằng chứng (gọi thẳng API, endpoint tự-báo-cáo) thay vì đoán; đừng dừng ở lớp đầu tiên.

## Nghiệm thu qua cloud (đạt)
- https://<domain>/ping -> "ok".
- Cloud nối Supabase: POST /auth/login trả userId (đọc đúng user trong DB Supabase).
- App trỏ baseUrl = domain HTTPS, gỡ usesCleartextTraffic; build lại APK.
- Trên máy thật QUA INTERNET (không LAN, không adb reverse): login -> token thật ~142 ký tự; transfer qua domain cloud -> banner heads-up "Biến động số dư +2.000.000đ" nhảy trên màn hình. Đổi mạng 4G/WiFi gì cũng chạy.
- Email OTP qua Brevo HTTP API: sau khi gỡ chặn IP + nạp đúng BREVO_API_KEY -> Brevo trả 201, OTP về inbox thật (Brevo Logs hiện Delivered). Cả 4 kênh (web/DB/push/email) đều chạy qua internet.

## Lưu ý vận hành
- Tên biến env phải ĐÚNG HOA: BREVO_API_KEY, BREVO_SENDER, DB_URL... (Linux phân biệt hoa/thường; Spring map brevo.api-key <- BREVO_API_KEY). Đặt sai hoa/thường là biến không vào -> email 401.
- BREVO_SENDER phải là email đã verify trong Brevo, nếu không Brevo trả lỗi sender.
- Railway free trial có hạn mức credit; hết credit service tắt -> cân nhắc nâng plan khi cần demo lâu dài.
