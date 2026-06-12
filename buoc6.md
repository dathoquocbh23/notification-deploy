## Mục tiêu
Hoàn tất 2 kịch bản còn lại của "Demo 2 — gửi một-tới-nhiều và đường không-server": (8) broadcast qua topic và (9) serverless bằng Supabase Edge Function. Cả hai đều dùng CHUNG một vận chuyển FCM, chỉ khác NƠI đặt logic điều phối.

## Luồng 8 — Broadcast qua topic (một-tới-nhiều)
Token = địa chỉ MỘT máy; Topic = kênh phát thanh, máy nào subscribe thì nhận. Broadcast không lặp token: chỉ bắn 1 message tới topic, FCM tự phát cho mọi máy đã subscribe.
- App: push_service.dart gọi subscribeToTopic('all') lúc init -> mỗi máy mở app tự vào "đài" 'all'.
- Backend: POST /demo/promo -> NotificationService.broadcastPromo() -> FcmService.sendToTopic('all', title, body). (Đã thêm channel_id banking_channel + priority HIGH nên promo cũng nhảy heads-up.)
- Nghiệm thu: gọi qua backend cloud
```
curl -X POST https://notification-deploy-production.up.railway.app/demo/promo \
  -H "Content-Type: application/json" -d '{"title":"...","body":"..."}'
-> {"status":"sent","target":"topic:all"}  => mọi máy subscribe 'all' cùng nổ banner
```
Lưu ý nhỏ: khi gọi từ PowerShell phải ép body UTF-8 (emoji/tiếng Việt), nếu không Brevo/HTTP nhận JSON hỏng -> 400. Đã làm sẵn function Send-Promo dùng [Encoding]::UTF8.GetBytes.

## Luồng 9 — Serverless: Supabase Edge Function (không server của nhóm chạy)
Đường thay thế cho backend tự nuôi: thay vì Spring Boot trên Railway điều phối, để Supabase tự bắn khi DB đổi. CẢ HAI đường đều qua FCM — FCM là vận chuyển, không phải "bên thứ ba".
Luồng: INSERT vào bảng transactions (Postgres Supabase) -> Database trigger (pg_net) gọi Edge Function -> function lấy access token từ service account, gọi FCM HTTP v1 -> banner. Không có server nào của mình chạy.

Các bước đã dựng:
- Code: supabase/functions/notify-on-transaction/index.ts (Deno.serve, đọc record.to_token, gọi messages:send).
- Tạo bảng: transactions(id, to_token, amount, from_name, created_at) trên Supabase.
- Deploy function: supabase functions deploy notify-on-transaction --no-verify-jwt (qua Supabase CLI + Personal Access Token).
- Set secret: supabase secrets set FCM_SERVICE_ACCOUNT=<nội dung service-account.json> (function đọc Deno.env.get).
- Tạo webhook bằng SQL: extension pg_net + trigger trg_notify_transaction AFTER INSERT trên transactions -> net.http_post tới URL function với body {type:'INSERT', record: row_to_json(NEW)}.

Nghiệm thu (2 mức):
- Gọi THẲNG function với token thật device.an -> HTTP 200, body {"fcmStatus":200} -> máy nổ banner.
- INSERT vào transactions (mô phỏng SQL editor) -> trigger tự gọi function; pg_net ghi lại phản hồi trong net._http_response: status_code=200, content {"fcmStatus":200} -> máy nổ banner "+246.000đ từ Serverless-Insert". Trọn luồng INSERT -> webhook -> Edge Function -> FCM, không server của nhóm.

## So sánh hai đường (điểm kiến trúc của slide)
- Tự build (Spring Boot/Railway): mình nuôi server, toàn quyền nghiệp vụ (ghi DB trước, fan-out, dọn token chết), nhưng phải vận hành/để ý credit.
- Serverless (Supabase Edge Function): không nuôi server, DB đổi là tự bắn; đổi lại logic nằm trong hệ Supabase, phụ thuộc nền tảng.
- Điểm chung BẮT BUỘC của cả hai: đều đẩy qua FCM. Khác nhau chỉ là NƠI đặt logic điều phối.

## Ghi chú vận hành
- Edge Function deploy bằng Supabase CLI cần Personal Access Token (toàn quyền tài khoản) -> revoke sau khi xong.
- Trigger dùng pg_net (net.http_post) là bất đồng bộ: INSERT trả về ngay, request HTTP chạy nền; kiểm tra kết quả ở bảng net._http_response.
- Function deploy với --no-verify-jwt để webhook/DB gọi được không cần JWT.
