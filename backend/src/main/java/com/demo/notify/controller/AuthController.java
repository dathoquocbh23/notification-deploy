package com.demo.notify.controller;

import com.demo.notify.entity.AppUser;
import com.demo.notify.entity.UserToken;
import com.demo.notify.repo.TokenRepo;
import com.demo.notify.repo.UserRepo;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SD-1 · VÒNG ĐỜI TOKEN. Auth GIẢ LẬP (không JWT/mật khẩu thật) —
 * trọng tâm là 3 thao tác với bảng user_tokens:
 *   login  -> INSERT token
 *   refresh-> UPDATE token
 *   logout -> DELETE token  (không xoá = máy cũ vẫn nhận noti = lỗ hổng)
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepo userRepo;
    private final TokenRepo tokenRepo;

    public AuthController(UserRepo userRepo, TokenRepo tokenRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
    }

    public record LoginReq(String email, String displayName,
                           String fcmToken, String deviceInfo) {}

    /** Đăng nhập (giả lập) + ĐĂNG KÝ TOKEN cho thiết bị này. */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        AppUser user = userRepo.findByEmail(req.email()).orElseGet(() -> {
            AppUser u = new AppUser();
            u.email = req.email();
            u.displayName = req.displayName();
            return userRepo.save(u);
        });

        // Token đã tồn tại (cài lại app, login lại) -> gắn về user hiện tại
        UserToken t = tokenRepo.findByToken(req.fcmToken())
                .orElseGet(UserToken::new);
        t.userId = user.id;
        t.token = req.fcmToken();
        t.deviceInfo = req.deviceInfo();
        tokenRepo.save(t);

        return Map.of("userId", user.id, "email", user.email,
                      "displayName", user.displayName == null ? "" : user.displayName);
    }

    public record RefreshReq(String oldToken, String newToken) {}

    /** FCM tự đổi token (onTokenRefresh) -> app báo server cập nhật. */
    @PutMapping("/fcm-token")
    public Map<String, String> refresh(@RequestBody RefreshReq req) {
        tokenRepo.findByToken(req.oldToken()).ifPresent(t -> {
            t.token = req.newToken();
            tokenRepo.save(t);
        });
        return Map.of("status", "ok");
    }

    public record LogoutReq(String fcmToken) {}

    /** Đăng xuất -> XOÁ token thiết bị này. Bước bảo mật bắt buộc. */
    @PostMapping("/logout")
    public Map<String, String> logout(@RequestBody LogoutReq req) {
        tokenRepo.deleteByToken(req.fcmToken());
        return Map.of("status", "ok");
    }
}
