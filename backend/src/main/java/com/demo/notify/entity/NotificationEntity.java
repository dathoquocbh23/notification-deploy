package com.demo.notify.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Bảng notifications — NGUỒN CHÂN LÝ của thông báo.
 * Luôn INSERT vào đây TRƯỚC khi push. Push chỉ là "kênh đánh thức";
 * màn hình lịch sử trong app đọc từ bảng này nên không bao giờ thiếu,
 * kể cả khi push bị rớt (token chết, mất mạng, user tắt quyền noti).
 */
@Entity @Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    public String title;
    @Column(length = 1000)
    public String body;
    public String type;           // TRANSFER_OUT / TRANSFER_IN / PROMO ...
    public Instant createdAt = Instant.now();
    public Instant readAt;        // null = chưa đọc (chấm đỏ)
}
