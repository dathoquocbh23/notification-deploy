package com.demo.notify.service;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

/**
 * TẦNG VẬN CHUYỂN PUSH — gói mọi lời gọi FCM HTTP v1 (qua firebase-admin).
 *
 * Lưu ý kiến trúc: file này KHÔNG biết gì về user, DB, hay nghiệp vụ.
 * Nó chỉ nhận (token | topic) + nội dung và gửi đi. Phần "điều phối"
 * (gửi cho ai, ghi DB, dọn token chết) nằm ở NotificationService.
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    /**
     * Gửi tới MỘT token (nhắm 1 thiết bị — SD-2/SD-3).
     * @param onDeadToken callback khi FCM báo token đã chết (UNREGISTERED)
     *                    — để tầng trên xoá khỏi DB (SD-1, dọn rác).
     */
    public void sendToToken(String token, String title, String body,
                            Map<String, String> data, Consumer<String> onDeadToken) {
        Message msg = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title).setBody(body).build())
                // Để banner NHẢY heads-up cả khi app ở nền: OS render trên đúng
                // channel Importance.max của app (banking_channel) + ưu tiên HIGH.
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("banking_channel").build())
                        .build())
                .putAllData(data == null ? Map.of() : data)
                .build();
        try {
            String id = FirebaseMessaging.getInstance().send(msg);
            log.info("FCM OK -> token={}... msgId={}", token.substring(0, 12), id);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("Token chết (user gỡ app?) -> báo tầng trên dọn DB");
                onDeadToken.accept(token);
            } else {
                log.error("FCM lỗi: {}", e.getMessagingErrorCode(), e);
            }
        }
    }

    /**
     * Gửi tới MỘT TOPIC (nhắm cả nhóm — broadcast khuyến mãi).
     * Token = địa chỉ 1 máy; Topic = kênh phát thanh, máy nào subscribe thì nhận.
     */
    public void sendToTopic(String topic, String title, String body) {
        Message msg = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title).setBody(body).build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setChannelId("banking_channel").build())
                        .build())
                .build();
        try {
            FirebaseMessaging.getInstance().send(msg);
            log.info("FCM broadcast OK -> topic={}", topic);
        } catch (FirebaseMessagingException e) {
            log.error("FCM topic lỗi", e);
        }
    }
}
