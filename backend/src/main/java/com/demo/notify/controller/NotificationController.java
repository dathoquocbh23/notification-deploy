package com.demo.notify.controller;

import com.demo.notify.entity.NotificationEntity;
import com.demo.notify.repo.NotificationRepo;
import com.demo.notify.repo.UserRepo;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * SD-3 · Màn hình lịch sử thông báo đọc từ ĐÂY (DB), không phải từ push.
 * Push = kênh đánh thức; bảng notifications = nguồn chân lý.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;

    public NotificationController(NotificationRepo n, UserRepo u) {
        this.notificationRepo = n;
        this.userRepo = u;
    }

    @GetMapping
    public List<NotificationEntity> list(@RequestParam String email) {
        Long userId = userRepo.findByEmail(email).orElseThrow().id;
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@RequestParam String email) {
        Long userId = userRepo.findByEmail(email).orElseThrow().id;
        return Map.of("count", notificationRepo.countByUserIdAndReadAtIsNull(userId));
    }

    @PostMapping("/{id}/read")
    public Map<String, String> markRead(@PathVariable Long id) {
        notificationRepo.findById(id).ifPresent(n -> {
            n.readAt = Instant.now();
            notificationRepo.save(n);
        });
        return Map.of("status", "ok");
    }
}
