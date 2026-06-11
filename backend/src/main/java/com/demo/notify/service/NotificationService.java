package com.demo.notify.service;

import com.demo.notify.entity.NotificationEntity;
import com.demo.notify.entity.UserToken;
import com.demo.notify.repo.NotificationRepo;
import com.demo.notify.repo.TokenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  ⭐ NOTIFICATION SERVICE — TẦNG ĐIỀU PHỐI (trái tim của bài)
 * ============================================================
 *
 * Đây chính là cái hộp "Notification Service" trong sơ đồ pipeline:
 *
 *   Sự kiện ──► [ NotificationService ] ──┬──► Push (FCM)
 *                                          └──► Email (SMTP)
 *
 * Quy trình CHUẨN cho mọi thông báo (đi từng bước trong demo):
 *   1. GHI DB TRƯỚC  — bảng notifications là nguồn chân lý.
 *      Push có thể rớt; lịch sử trong app đọc từ DB nên không bao giờ thiếu.
 *   2. LẤY MỌI TOKEN — 1 user có N thiết bị (bảng user_tokens).
 *   3. FAN-OUT       — bắn qua từng kênh. Token chết thì dọn luôn (SD-1).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepo notificationRepo;
    private final TokenRepo tokenRepo;
    private final FcmService fcm;

    public NotificationService(NotificationRepo notificationRepo,
                               TokenRepo tokenRepo, FcmService fcm) {
        this.notificationRepo = notificationRepo;
        this.tokenRepo = tokenRepo;
        this.fcm = fcm;
    }

    /**
     * Gửi thông báo tới MỘT user — dùng cho biến động số dư, biên lai...
     * (SD-2 bước "TRỌNG TÂM", SD-3 toàn bộ.)
     */
    public void notifyUser(Long userId, String type, String title, String body) {
        // ── BƯỚC 1: ghi DB trước — nguồn chân lý ──────────────────────
        NotificationEntity n = new NotificationEntity();
        n.userId = userId;
        n.type = type;
        n.title = title;
        n.body = body;
        notificationRepo.save(n);
        log.info("[1/3] Đã ghi notifications (id={}) cho user={}", n.id, userId);

        // ── BƯỚC 2: lấy TẤT CẢ token của user (N thiết bị) ────────────
        List<UserToken> tokens = tokenRepo.findByUserId(userId);
        log.info("[2/3] User {} có {} thiết bị", userId, tokens.size());

        // ── BƯỚC 3: fan-out push; token chết -> xoá khỏi DB ───────────
        for (UserToken t : tokens) {
            fcm.sendToToken(
                    t.token, title, body,
                    Map.of("notificationId", String.valueOf(n.id), "type", type),
                    deadToken -> {
                        tokenRepo.deleteByToken(deadToken);
                        log.info("Đã dọn token chết khỏi user_tokens");
                    });
        }
        log.info("[3/3] Fan-out xong cho user={}", userId);
    }

    /**
     * Broadcast tới TOÀN BỘ user qua topic — dùng cho khuyến mãi.
     * Khác notifyUser: KHÔNG lặp token — FCM tự phát cho mọi máy đã
     * subscribe topic. Token = 1 máy; Topic = kênh phát thanh.
     */
    public void broadcastPromo(String title, String body) {
        // Vẫn ghi DB cho từng user nếu muốn promo xuất hiện trong lịch sử.
        // Demo: ghi 1 bản ghi "đại diện" với userId=null cho gọn; sản phẩm
        // thật sẽ fan-out ghi theo segment.
        NotificationEntity n = new NotificationEntity();
        n.userId = null;
        n.type = "PROMO";
        n.title = title;
        n.body = body;
        notificationRepo.save(n);

        fcm.sendToTopic("all", title, body);
        log.info("Broadcast PROMO -> topic 'all'");
    }
}
