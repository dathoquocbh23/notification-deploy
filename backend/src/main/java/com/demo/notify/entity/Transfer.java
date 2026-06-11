package com.demo.notify.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Nghiệp vụ GIẢ LẬP — không ledger, không số dư thật. Chỉ là cái cớ phát sự kiện. */
@Entity @Table(name = "transfers")
public class Transfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long fromUserId;
    public Long toUserId;
    public Long amount;           // VND
    public String status = "PENDING";   // PENDING -> SUCCESS
    public String otpCode;        // demo: lưu thẳng 6 số (thực tế phải hash)
    public Instant otpExpiresAt;
    public Instant createdAt = Instant.now();
}
