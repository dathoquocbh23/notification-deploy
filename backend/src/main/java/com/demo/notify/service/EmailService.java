package com.demo.notify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * TẦNG VẬN CHUYỂN EMAIL — Brevo HTTP API (POST /v3/smtp/email).
 *
 * Vì sao HTTP API chứ không SMTP: server cloud (Railway...) CHẶN cổng SMTP
 * outbound (25/587/465/2525) → JavaMailSender/SMTP treo timeout. HTTP API đi
 * cổng 443 nên thoát. Gốc rễ là GIAO THỨC, không phải nhà cung cấp: Brevo qua
 * SMTP relay vẫn bị chặn trên cloud.
 *
 * Kênh thay thế cùng vị trí pipeline: SMS gateway — CHỈ đổi class này, phần
 * điều phối ở NotificationService/DemoController giữ nguyên (vận chuyển tách
 * khỏi điều phối → chữ ký send/sendOtp/sendReceipt không đổi).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final RestClient client;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            @Value("${brevo.api-key}") String apiKey,
            @Value("${brevo.sender-email}") String senderEmail,
            @Value("${brevo.sender-name}") String senderName) {
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3/smtp/email")
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", "application/json")
                .build();
    }

    @Async  // không chặn request — email chậm hơn push nhiều
    public void send(String to, String subject, String body) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", body);
        try {
            var resp = client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Brevo OK → to={} status={}", to, resp.getStatusCode().value());
        } catch (RestClientResponseException e) {
            // 4xx: Brevo trả lý do trong body (vd sender chưa verify / sai api-key) — log nguyên văn
            log.error("Brevo lỗi {} → {}", e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Brevo gọi thất bại: {}", e.getMessage());
        }
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
