## Mục tiêu
Cấu hình backend nối vào Supabase qua Session pooler và khởi động thành công, chỉ chỉnh application.yml + .env (không sửa logic Java).

## Vì sao BỎ giá trị default localhost (chống "lỗi câm")
Trước đây datasource khai dạng ${DB_URL:jdbc:postgresql://localhost:5432/notify_demo}. Nếu quên nạp biến môi trường, Spring sẽ ÂM THẦM rơi về localhost và "chạy được" trên một DB sai/không tồn tại — lỗi rất khó phát hiện vì ứng dụng không báo gì.
- Đã đổi thành ${DB_URL}, ${DB_USER}, ${DB_PASSWORD} — KHÔNG còn default.
- Thiếu biến môi trường thì app fail-fast ngay khi khởi động, thay vì chạy nhầm DB.

## Vì sao maximum-pool-size = 4
- Supabase free tier giới hạn số kết nối đồng thời rất thấp; pool lớn sẽ làm cạn slot kết nối của cả project.
- Bài học pool overflow: pool mặc định (10) dễ vượt hạn mức pooler -> kết nối bị từ chối, request treo. Giữ pool nhỏ (4) đủ cho demo và an toàn với hạn mức.

## Vì sao Session pooler (5432) chứ không phải Transaction pooler (6543)
- JDBC/Hibernate dùng PREPARED STATEMENT có tên (S_1, S_2...). Transaction pooler (port 6543) ghép nhiều client trên một kết nối theo từng transaction, KHÔNG giữ trạng thái session -> prepared statement vỡ.
- Session pooler (port 5432) cấp một session riêng cho mỗi kết nối, giữ nguyên trạng thái -> Hibernate chạy bình thường. Vì vậy DB_URL trỏ cổng 5432, user dạng postgres.<project-ref>.

## Bảng chẩn đoán lỗi DB (tham chiếu nhanh)
- UnknownHost / không phân giải được host -> đang nhầm sang Direct connection (db.<ref>.supabase.co) thay vì host pooler aws-1-...pooler.supabase.com.
- password authentication failed -> DB_USER thiếu hậu tố project-ref (phải là postgres.<project-ref>, không phải postgres trơn).
- ERROR: prepared statement "S_1" already exists -> đang nối nhầm Transaction pooler cổng 6543; đổi về Session pooler cổng 5432.

## Lỗi THẬT gặp khi chạy
Kết nối DB qua Session pooler đạt ngay lần đầu (HikariPool Added connection + Start completed, Hibernate chạy DDL trên Supabase). Tuy nhiên app vẫn không khởi động được vì một lỗi khác:
BUG: APPLICATION FAILED TO START — "Parameter 0 of constructor in com.demo.notify.service.EmailService required a bean of type 'org.springframework.mail.javamail.JavaMailSender' that could not be found."
Nguyên nhân: đã xoá block spring.mail nhưng EmailService.java vẫn inject cứng JavaMailSender; Spring Boot chỉ tạo bean JavaMailSender khi có spring.mail.host.
Cách sửa (không đụng Java): cấu hình lại spring.mail trỏ Brevo SMTP relay smtp-relay.brevo.com cổng 2525 (cổng SMTP thay thế, không bị chặn như 587/465/25), creds đặt trong backend/.env. Sau đó bean JavaMailSender được tạo, DI thoả, app start.

## Kết quả nghiệm thu
- HikariPool-1 - Start completed.
- Tomcat started on port 8080.
- Started NotifyApplication in ~5 giây.
- GET /ping trả về HTTP 200, body "ok" (thêm PingController tối giản làm health-check — ngoại lệ Java duy nhất được phép ở bước này).

## Lệnh chạy backend (PowerShell)
Vì secret (mật khẩu DB, creds Brevo) nằm trong backend/.env và KHÔNG commit, ta nạp chúng vào biến môi trường ngay trước khi chạy. Đoạn lệnh dưới đọc từng dòng .env và đặt thành biến môi trường của phiên PowerShell, sau đó khởi động Spring Boot:
```
cd e:\fpt_university\Semester8\PRM393\notification\backend
Get-Content .env | Where-Object { $_ -match '^\s*[^#].+=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2
  Set-Item -Path "Env:$($k.Trim())" -Value $v.Trim()
}
mvn spring-boot:run
```
Giải thích từng phần để mọi người hiểu:
- Get-Content .env: đọc toàn bộ file .env thành từng dòng.
- Where-Object { $_ -match '^\s*[^#].+=' }: bỏ qua dòng trống và dòng chú thích (bắt đầu bằng #), chỉ giữ dòng dạng KEY=VALUE.
- $k, $v = $_ -split '=', 2: tách mỗi dòng thành tên biến ($k) và giá trị ($v); tham số 2 đảm bảo chỉ tách ở dấu = ĐẦU TIÊN, nên giá trị có chứa dấu = (vd chuỗi kết nối, key Brevo) vẫn nguyên vẹn.
- Set-Item -Path "Env:$($k.Trim())" -Value $v.Trim(): đặt biến môi trường tên $k giá trị $v (Trim để bỏ khoảng trắng thừa). Spring Boot tự ánh xạ DB_URL/DB_USER/... vào application.yml qua ${...}.
- mvn spring-boot:run: build và chạy ứng dụng; biến môi trường vừa nạp được Spring đọc lúc khởi động.
- Lưu ý: biến chỉ sống trong cửa sổ PowerShell hiện tại, đóng cửa sổ là mất — an toàn, không lưu secret ra đâu khác. Nhấn Ctrl+C để dừng backend cho Java thoát hẳn (tránh kẹt cổng 8080 lần sau).
