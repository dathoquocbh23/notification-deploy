package com.demo.notify.controller;

import com.demo.notify.entity.AppUser;
import com.demo.notify.entity.Transfer;
import com.demo.notify.repo.TransferRepo;
import com.demo.notify.repo.UserRepo;
import com.demo.notify.service.EmailService;
import com.demo.notify.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * SD-2 / SD-3 · Kịch bản demo. NGHIỆP VỤ GIẢ LẬP TỐI ĐA —
 * không ledger, không số dư. Transfer chỉ là CÁI CỚ PHÁT SỰ KIỆN.
 * Mọi thứ đáng xem nằm ở NotificationService.
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private final UserRepo userRepo;
    private final TransferRepo transferRepo;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public DemoController(UserRepo userRepo, TransferRepo transferRepo,
                          NotificationService notificationService,
                          EmailService emailService) {
        this.userRepo = userRepo;
        this.transferRepo = transferRepo;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    public record TransferReq(String fromEmail, String toEmail, Long amount) {}

    /** Bước 1: khởi tạo chuyển tiền -> sinh OTP -> EMAIL (kênh 1). */
    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> create(@RequestBody TransferReq req) {
        AppUser from = userRepo.findByEmail(req.fromEmail()).orElseThrow();
        AppUser to = userRepo.findByEmail(req.toEmail()).orElseThrow();

        // "Nghiệp vụ" 3 dòng:
        Transfer t = new Transfer();
        t.fromUserId = from.id;
        t.toUserId = to.id;
        t.amount = req.amount();
        t.otpCode = String.format("%06d", random.nextInt(1_000_000));
        t.otpExpiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        transferRepo.save(t);

        // SỰ KIỆN "cần xác thực" -> kênh EMAIL
        // (kênh thay thế: SMS gateway — cùng vị trí pipeline)
        emailService.sendOtp(from.email, t.otpCode);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("transferId", t.id, "status", "PENDING",
                             "message", "OTP đã gửi về email"));
    }

    public record VerifyReq(String otp) {}

    /** Bước 2: xác thực OTP -> SỰ KIỆN "giao dịch thành công" -> fan-out. */
    @PostMapping("/transfers/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id,
                                                      @RequestBody VerifyReq req) {
        Transfer t = transferRepo.findById(id).orElseThrow();

        if (!t.otpCode.equals(req.otp()) || Instant.now().isAfter(t.otpExpiresAt)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "OTP sai hoặc đã hết hạn"));
        }
        t.status = "SUCCESS";   // giả lập — không trừ/cộng tiền thật
        transferRepo.save(t);

        AppUser from = userRepo.findById(t.fromUserId).orElseThrow();
        AppUser to = userRepo.findById(t.toUserId).orElseThrow();
        String amount = String.format("%,d", t.amount);

        // ===== TRỌNG TÂM: SỰ KIỆN -> NOTIFICATION SERVICE -> FAN-OUT =====
        // Người gửi: push (SD-2 — foreground bridge) + email biên lai
        notificationService.notifyUser(from.id, "TRANSFER_OUT",
                "Chuyển tiền thành công",
                "-" + amount + "đ tới " + to.displayName + ". Số dư đã cập nhật.");
        emailService.sendReceipt(from.email, t.amount, to.displayName);

        // Người nhận: push (SD-3 — hero moment, app đã tắt vẫn nhận)
        notificationService.notifyUser(to.id, "TRANSFER_IN",
                "Biến động số dư",
                "+" + amount + "đ từ " + from.displayName + ". Số dư đã cập nhật.");

        return ResponseEntity.ok(Map.of("transferId", t.id, "status", "SUCCESS"));
    }

    public record PromoReq(String title, String body) {}

    /** Broadcast khuyến mãi -> TOPIC (1-nhiều). Cả lớp thấy 2 máy cùng nổ. */
    @PostMapping("/promo")
    public Map<String, String> promo(@RequestBody PromoReq req) {
        notificationService.broadcastPromo(
                req.title() == null ? "🎁 Ưu đãi hôm nay" : req.title(),
                req.body() == null ? "Hoàn 50% phí chuyển tiền đến hết tuần!" : req.body());
        return Map.of("status", "sent", "target", "topic:all");
    }
}
