## Mục tiêu
Chứng minh tầng điều phối (NotificationService + vòng đời token) chạy đúng CHỈ bằng curl + truy vấn DB, KHÔNG cần app Flutter. Push FCM cố tình fail (token giả) để soi xem lịch sử trong DB có còn đủ không.

## Pha 1 — Vòng đời token (INSERT / UPDATE / DELETE)
Ba thao tác trên bảng user_tokens trả lời câu hỏi "gửi push tới máy nào".
```
# (a) login -> INSERT token cho thiết bị
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"...","displayName":"An","fcmToken":"FAKE_TOKEN_1","deviceInfo":"curl-test"}'
# (b) onTokenRefresh -> UPDATE (cùng id, đổi token)
curl -X PUT http://localhost:8080/auth/fcm-token -H "Content-Type: application/json" \
  -d '{"oldToken":"FAKE_TOKEN_1","newToken":"FAKE_TOKEN_2"}'
# (c) logout -> DELETE (chốt bảo mật: máy cũ hết nhận noti)
curl -X POST http://localhost:8080/auth/logout -H "Content-Type: application/json" \
  -d '{"fcmToken":"FAKE_TOKEN_2"}'
```
Kết quả đối chiếu DB:
- Sau (a): user_tokens có 1 dòng id=1, token=FAKE_TOKEN_1 -> INSERT đúng.
- Sau (b): VẪN id=1, token đổi thành FAKE_TOKEN_2 -> UPDATE đúng (không tạo dòng mới).
- Sau (c): không còn dòng nào -> DELETE đúng. Đây là chốt bảo mật: không xoá token thì máy cũ vẫn nhận biến động số dư của tài khoản đã đăng xuất.

## Pha 2 — Sự kiện transfer (OTP qua email)
```
# login lại An + Bình (mỗi máy 1 token giả), rồi tạo lệnh chuyển
curl -X POST http://localhost:8080/demo/transfers -H "Content-Type: application/json" \
  -d '{"fromEmail":"<An>","toEmail":"<Binh>","amount":100000}'
# -> {"transferId":1,"status":"PENDING","message":"OTP đã gửi về email"}
# lấy OTP thẳng từ DB (không cần đọc mail)
#   select id, status, otp_code from transfers;  -> otp_code = 243206
curl -X POST http://localhost:8080/demo/transfers/1/verify -H "Content-Type: application/json" \
  -d '{"otp":"243206"}'   # -> {"status":"SUCCESS"}
```
- Transfer được tạo (status PENDING), OTP sinh và lưu DB; verify đúng OTP -> status SUCCESS -> phát sự kiện "giao dịch thành công" cho cả hai phía.
- LƯU Ý về email: backend log KHÔNG có lỗi mail, nhưng email vẫn KHÔNG về inbox — xem BUG bên dưới (thiếu From). Vì OTP lấy thẳng từ DB nên bước verify không phụ thuộc email.

## Pha 3 — Fan-out: GHI DB TRƯỚC, push chỉ là kênh đánh thức
Log backend in đủ 3 bước cho TỪNG user:
```
[1/3] Đã ghi notifications (id=1) cho user=1
[2/3] User 1 có 1 thiết bị
ERROR FcmService : FCM lỗi: INVALID_ARGUMENT (token giả)
[3/3] Fan-out xong cho user=1
[1/3] Đã ghi notifications (id=2) cho user=2
[2/3] User 2 có 1 thiết bị
ERROR FcmService : FCM lỗi: INVALID_ARGUMENT (token giả)
[3/3] Fan-out xong cho user=2
```
Đối chiếu DB bảng notifications: ĐÚNG 2 dòng — user 1 (An) TRANSFER_OUT, user 2 (Bình) TRANSFER_IN.
- Đây là minh chứng SỐNG cho nguyên tắc: ghi DB TRƯỚC rồi mới push. Push FCM fail hoàn toàn (token giả) nhưng bảng notifications vẫn đủ dòng -> màn hình lịch sử trong app vẫn hiển thị đầy đủ. DB là NGUỒN CHÂN LÝ, push chỉ là KÊNH ĐÁNH THỨC.

## BUG 1 — Token giả không kích hoạt dọn token chết
BUG: Token giả KHÔNG kích hoạt nhánh dọn token chết. FCM trả mã INVALID_ARGUMENT ("The registration token is not a valid FCM registration token"), KHÔNG phải UNREGISTERED. Mà FcmService chỉ gọi onDeadToken khi mã là UNREGISTERED, nên cả FAKE_TOKEN_A/FAKE_TOKEN_B vẫn còn nguyên trong user_tokens sau fan-out.
- Vì sao: INVALID_ARGUMENT = token SAI ĐỊNH DẠNG; UNREGISTERED = token ĐÚNG ĐỊNH DẠNG nhưng thiết bị đã gỡ app/token hết hiệu lực. Đây là hai tình huống khác nhau và việc chỉ dọn khi UNREGISTERED là CỐ Ý (không xoá nhầm token vì lỗi tạm thời).
- Hệ quả cho demo: muốn quan sát đúng nhánh "UNREGISTERED -> DELETE" thì phải dùng một FCM token THẬT đã hết hiệu lực (gỡ app rồi gửi lại), không thể dùng chuỗi giả.

## BUG 2 — Email không về inbox: thiếu From (Brevo đánh Error)
BUG: Email OTP/biên lai KHÔNG về inbox dù backend log sạch. Nguyên nhân: EmailService (bản SMTP) không set From — SimpleMailMessage chỉ có setTo/setSubject/setText. Brevo nhận ở tầng SMTP (250) nên backend không thấy exception, NHƯNG sau đó Brevo đánh Error vì email From rỗng (sender bắt buộc phải verify).
- Bằng chứng trên Brevo Logs: 2 dòng "[NotifyDemo] Mã xác thực giao dịch" và "[NotifyDemo] Biên lai giao dịch" có Events = Error, cột From = None.
- Cách sửa (tối thiểu, 2 file): thêm app.mail.from=${MAIL_FROM} vào application.yml; trong EmailService inject from và gọi msg.setFrom(from) với sender đã verify (visualedu.info@gmail.com).
- Sau khi sửa, bật mail.debug thấy hội thoại SMTP: MAIL FROM:<visualedu.info@gmail.com> -> 250 accepting; From: visualedu.info@gmail.com; 250 OK: queued -> Brevo chuyển trạng thái Delivered/Sent.

## Cách kiểm tra log trên Brevo
- Đăng nhập Brevo -> menu Transactional -> Email -> Logs (hoặc Statistics -> Email -> Logs).
- Lọc theo Recipient (To) bằng email người nhận, hoặc theo khoảng ngày.
- Mỗi email có chuỗi Events: Sent -> Delivered -> Opened... Nếu thấy Error nghĩa là bị từ chối; bấm icon con mắt để xem chi tiết lý do.
- Cột From rất quan trọng: From = None tức email thiếu sender -> chắc chắn lỗi. From phải là địa chỉ đã verify trong Senders.
- Mẹo: nếu backend log "gửi xong" mà Brevo vẫn không có dòng nào, kiểm tra api/SMTP credentials; nếu có dòng nhưng Error, đọc lý do (thiếu From, sender chưa verify, vượt hạn mức).

## Kết luận
- Vòng đời token: INSERT/UPDATE/DELETE đều đúng trên Supabase.
- Sự kiện transfer: OTP sinh + lưu DB; verify chuyển SUCCESS; email ban đầu lỗi do thiếu From, đã sửa và gửi được (Brevo 250 queued).
- Fan-out: ghi DB trước, push sau; push fail nhưng notifications đủ 2 dòng.
- Toàn bộ chứng minh KHÔNG cần app Flutter, chỉ curl + SQL. Hai bug thật được phát hiện và ghi nhận (điều kiện dọn token; thiếu From email).
