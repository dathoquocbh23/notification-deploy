package com.demo.notify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * TẦNG VẬN CHUYỂN EMAIL — Gmail SMTP qua JavaMailSender.
 * (Tái dùng pattern từ luồng OTP của auth service.)
 *
 * Kênh thay thế cùng vị trí trong pipeline: SMS gateway (eSMS/Twilio) —
 * chỉ đổi class này, phần điều phối ở NotificationService giữ nguyên.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;   // sender đã verify trong Brevo — thiếu thì Brevo đánh Error

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async  // không chặn request — email chậm hơn push nhiều
    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);   // BẮT BUỘC — Brevo từ chối email không có From
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }

    public void sendOtp(String to, String otp) {
        send(to, "[NotifyDemo] Mã xác thực giao dịch",
             "Mã OTP của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút."
             + "\nTuyệt đối không chia sẻ mã này cho bất kỳ ai.");
    }

    public void sendReceipt(String to, long amount, String counterpart) {
        send(to, "[NotifyDemo] Biên lai giao dịch",
             "Bạn đã chuyển thành công " + String.format("%,d", amount)
             + "đ tới " + counterpart + ".\nCảm ơn bạn đã sử dụng dịch vụ.");
    }
}
