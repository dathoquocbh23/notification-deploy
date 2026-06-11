// ============================================================
//  SD-4 · ĐƯỜNG SERVERLESS — logic điều phối dời lên Supabase
// ============================================================
// Luồng: INSERT INTO transactions (Postgres trên Supabase)
//        -> Database Webhook (trigger on INSERT) gọi function này
//        -> function gọi FCM HTTP v1 -> banner trên thiết bị
//
// So với đường tự build (Spring Boot): KHÔNG nuôi server, DB đổi là
// tự bắn. Đổi lại logic nằm trong hệ Supabase, phụ thuộc nền tảng.
// CẢ HAI đường đều qua FCM — FCM là vận chuyển, không phải "bên thứ ba".
//
// Triển khai:
//   supabase functions deploy notify-on-transaction --no-verify-jwt
//   supabase secrets set FCM_SERVICE_ACCOUNT="$(cat service-account.json)"
// Tạo bảng + webhook:
//   create table transactions (
//     id bigserial primary key,
//     to_token text not null,        -- demo: lưu thẳng FCM token người nhận
//     amount bigint not null,
//     from_name text default 'Ai đó',
//     created_at timestamptz default now()
//   );
//   Dashboard -> Database -> Webhooks -> on INSERT transactions
//     -> gọi Edge Function notify-on-transaction
//
// Demo trong lớp: mở SQL editor, chạy
//   insert into transactions (to_token, amount, from_name)
//   values ('<FCM_TOKEN_MÁY_B>', 100000, 'An');
// -> máy B nổ banner. KHÔNG có server nào của mình chạy cả.

import { JWT } from "npm:google-auth-library@9";

const SCOPES = ["https://www.googleapis.com/auth/firebase.messaging"];

Deno.serve(async (req) => {
  try {
    // 1) Payload từ Database Webhook: { type:'INSERT', record:{...} }
    const payload = await req.json();
    const record = payload.record;
    if (!record?.to_token) {
      return new Response("missing to_token", { status: 400 });
    }

    // 2) Lấy access token gọi FCM v1 (OAuth2 từ service account)
    const sa = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT")!);
    const jwt = new JWT({
      email: sa.client_email,
      key: sa.private_key,
      scopes: SCOPES,
    });
    const { access_token } = await jwt.authorize();

    // 3) Gọi FCM HTTP v1 — y hệt đường tự build, chỉ khác NƠI gọi
    const amount = Number(record.amount).toLocaleString("vi-VN");
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${access_token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: record.to_token,
            notification: {
              title: "Biến động số dư (serverless)",
              body: `+${amount}đ từ ${record.from_name} — gửi bởi Supabase Edge Function`,
            },
          },
        }),
      },
    );

    const body = await res.text();
    console.log("FCM response:", res.status, body);
    return new Response(JSON.stringify({ fcmStatus: res.status }), {
      headers: { "Content-Type": "application/json" },
    });
  } catch (e) {
    console.error(e);
    return new Response(String(e), { status: 500 });
  }
});
