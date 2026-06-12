package com.demo.notify.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Khởi tạo firebase-admin CHỈ để gọi FCM (vận chuyển push).
 * Không dùng bất kỳ dịch vụ Firebase nào khác — đây là phần "bắt buộc"
 * mà mọi hướng (tự build / serverless) đều phải đi qua.
 *
 * Nguồn credential (ưu tiên env để chạy trên cloud — không cần file):
 *   - FIREBASE_SA_JSON: dán cả nội dung service-account.json vào biến môi trường.
 *   - fallback: đọc file tại firebase.service-account-path (chạy local).
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Value("${FIREBASE_SA_JSON:}")
    private String serviceAccountJson;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
        InputStream sa = (serviceAccountJson != null && !serviceAccountJson.isBlank())
                ? new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
                : new FileInputStream(serviceAccountPath);
        try (sa) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(sa))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
