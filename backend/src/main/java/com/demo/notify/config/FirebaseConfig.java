package com.demo.notify.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;

/**
 * Khởi tạo firebase-admin CHỈ để gọi FCM (vận chuyển push).
 * Không dùng bất kỳ dịch vụ Firebase nào khác — đây là phần "bắt buộc"
 * mà mọi hướng (tự build / serverless) đều phải đi qua.
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return FirebaseApp.getInstance();
        try (FileInputStream sa = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(sa))
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
