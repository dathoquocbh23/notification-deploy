## Mục tiêu
Dọn sạch các file nhạy cảm và rác trước khi khởi tạo Git, đảm bảo lần commit đầu tiên không lọt khoá bí mật.

## Đã dọn những gì
- Đổi tên file service account Firebase thành backend/service-account.json (đúng đường dẫn FIREBASE_SA_PATH mặc định trong application.yml).
- Chuyển google-services.json từ thư mục gốc về đúng vị trí app/android/app/google-services.json.
- Bổ sung .gitignore: *adminsdk*.json, database.txt, report_assets/.
- Xoá cây thư mục rác sinh do lệnh tạo thư mục bị lỗi.
- git init và tạo commit đầu tiên; xác nhận mọi file bí mật đều nằm trong vùng ignored.

## Vì sao phải đổi tên file service account
File gốc tên notify-demo-...-adminsdk-...json. Trong .gitignore chỉ có dòng "service-account.json", là so khớp theo ĐÚNG TÊN file nên không bắt được tên thật của khoá.
BUG: File private key service account đặt tên notify-demo-*-adminsdk-*.json KHÔNG khớp pattern .gitignore "service-account.json" -> nếu git init ngay thì khoá thật sẽ bị commit và lộ.
Khắc phục: đổi tên về service-account.json (đã có sẵn trong .gitignore) và thêm pattern phòng hờ *adminsdk*.json cho mọi khoá tải về sau này.

## Bug thư mục rác do PowerShell không brace-expand
BUG: Lệnh kiểu "mkdir -p {diagrams,backend/src/...}" chạy trong PowerShell không được khai triển dấu ngoặc nhọn như Bash, nên cả chuỗi "{diagrams,backend/.../notify-on-transaction}" bị coi là MỘT tên thư mục -> tạo ra cây thư mục rỗng tên rác lồng nhau.
Khắc phục: đã xoá toàn bộ cây thư mục rác này; cấu trúc thật của repo không bị ảnh hưởng.

## Kết quả kiểm tra
- git status --ignored xác nhận backend/service-account.json, app/android/ (chứa google-services.json) và database.txt đều thuộc vùng ignored.
- Không còn file lạ nào nằm ngoài vùng ignored; không có khoá bí mật nào được track.
