package com.demo.notify.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * SD-1 · Bảng user_tokens — 1 user có N thiết bị, mỗi thiết bị 1 FCM token.
 * Vòng đời: INSERT lúc login · UPDATE lúc onTokenRefresh · DELETE lúc logout
 * hoặc khi FCM trả UNREGISTERED (token chết).
 */
@Entity @Table(name = "user_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class UserToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    @Column(length = 512, nullable = false)
    public String token;
    public String deviceInfo;     // vd: "Samsung A52 / Android 14"
    public Instant createdAt = Instant.now();
}
