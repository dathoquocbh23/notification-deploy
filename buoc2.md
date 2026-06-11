## Mục tiêu
Xác nhận 4 entity JPA (AppUser, UserToken, NotificationEntity, Transfer) được Hibernate sinh đúng thành 4 bảng trên Supabase, đúng cột và ràng buộc — không phải tự viết SQL tạo bảng.

## Cách kiểm tra (SQL trên Supabase SQL Editor)
Dán các câu sau vào SQL Editor để soi metadata thật trong information_schema:
```
-- 1) 4 bảng có tồn tại trong schema public?
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in ('users','user_tokens','notifications','transfers')
order by table_name;

-- 2) Cột + kiểu dữ liệu của từng bảng
select table_name, column_name, data_type, is_nullable
from information_schema.columns
where table_schema = 'public'
  and table_name in ('users','user_tokens','notifications','transfers')
order by table_name, ordinal_position;

-- 3) Ràng buộc khóa chính / unique (email, token)
select tc.table_name, tc.constraint_type, kcu.column_name
from information_schema.table_constraints tc
join information_schema.key_column_usage kcu
  on tc.constraint_name = kcu.constraint_name
 and tc.table_schema = kcu.table_schema
where tc.table_schema = 'public'
  and tc.table_name in ('users','user_tokens','notifications','transfers')
  and tc.constraint_type in ('PRIMARY KEY','UNIQUE')
order by tc.table_name, tc.constraint_type;
```

## users — danh tính người dùng
- Cột: id (PK, identity), email (varchar, UNIQUE, NOT NULL), display_name (varchar).
- Vai trò: định danh chủ thể của mọi thông báo và giao dịch. Email là khóa nghiệp vụ để tra cứu user trong auth giả lập.
- Sinh từ entity AppUser; displayName -> display_name (Hibernate đổi camelCase sang snake_case).

## user_tokens — "gửi push tới MÁY NÀO"
- Cột: id (PK), user_id (int8), token (varchar, UNIQUE), device_info (varchar), created_at (timestamptz).
- Vai trò: trả lời đúng câu hỏi "đẩy thông báo tới thiết bị nào". MỘT user có N thiết bị, mỗi thiết bị một FCM token -> quan hệ 1-N (một user_id ứng nhiều dòng token).
- token UNIQUE để mỗi máy chỉ có một bản ghi; login INSERT, onTokenRefresh UPDATE, logout/UNREGISTERED DELETE (vòng đời token).
- Khi gửi push cho 1 user: lấy mọi token theo user_id rồi fan-out tới từng máy.

## notifications — "nguồn chân lý, ghi DB TRƯỚC khi push"
- Cột: id (PK), user_id (int8), title (varchar), body (varchar), type (varchar), created_at (timestamptz), read_at (timestamptz, nullable).
- Vai trò: là NGUỒN CHÂN LÝ của thông báo. Luồng chuẩn: ghi vào bảng này TRƯỚC, rồi mới push. Push chỉ là "kênh đánh thức" — có thể rớt (token chết, mất mạng, tắt quyền), nhưng lịch sử trong app đọc từ bảng này nên không bao giờ thiếu.
- read_at = null nghĩa là chưa đọc (chấm đỏ); type phân loại TRANSFER_IN / TRANSFER_OUT / PROMO.

## transfers — cái cớ phát sự kiện (nghiệp vụ giả lập)
- Cột: id (PK), from_user_id (int8), to_user_id (int8), amount (int8), status (varchar), otp_code (varchar), otp_expires_at (timestamptz), created_at (timestamptz).
- Vai trò: nghiệp vụ chuyển tiền GIẢ LẬP — không ledger, không số dư thật. Chỉ tồn tại để phát ra "sự kiện cần thông báo" (OTP khi tạo, biến động số dư khi verify).
- status PENDING -> SUCCESS; otp_code/otp_expires_at phục vụ bước xác thực (demo lưu thẳng OTP, thực tế phải hash).

## Lưu ý vận hành
- ddl-auto=update CHỈ dùng cho demo (Hibernate tự tạo/chỉnh bảng cho nhanh). Production phải dùng migration có kiểm soát (Flyway/Liquibase) để thay đổi schema an toàn, có lịch sử và rollback.

## Kết quả xác nhận
- Cả 4 bảng users, user_tokens, notifications, transfers đều xuất hiện trên Supabase (Schema Visualizer), đúng cột và đúng ràng buộc UNIQUE (email, token), khóa chính identity int8.
- Mapping entity -> bảng khớp 100%, KHÔNG phát sinh lỗi mapping nên không cần sửa code Java.
