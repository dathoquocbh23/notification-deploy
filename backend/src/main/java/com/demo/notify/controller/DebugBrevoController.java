package com.demo.notify.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * TẠM — chẩn đoán Brevo trên cloud (xem key có nạp không + Brevo trả gì).
 * XOÁ sau khi chẩn đoán xong.
 */
@RestController
public class DebugBrevoController {

    @Value("${brevo.api-key:}")
    private String key;
    @Value("${brevo.sender-email:}")
    private String sender;

    @GetMapping("/debug/brevo")
    public Map<String, Object> debug(@RequestParam(defaultValue = "dathoquocdn23@gmail.com") String to) {
        int keyLen = key == null ? 0 : key.length();
        String keyPrefix = (key != null && key.length() >= 9) ? key.substring(0, 9) : String.valueOf(key);
        if (keyLen == 0) {
            return Map.of("keyLen", 0, "sender", sender,
                          "note", "BREVO_API_KEY RỖNG trên instance đang chạy");
        }
        RestClient c = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3/smtp/email")
                .defaultHeader("api-key", key)
                .build();
        try {
            var r = c.post().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sender", Map.of("name", "Vi Demo", "email", sender),
                                 "to", List.of(Map.of("email", to)),
                                 "subject", "[NotifyDemo] Debug tu cloud",
                                 "textContent", "debug from railway"))
                    .retrieve().toBodilessEntity();
            return Map.of("keyLen", keyLen, "keyPrefix", keyPrefix, "sender", sender,
                          "brevoStatus", r.getStatusCode().value());
        } catch (RestClientResponseException e) {
            return Map.of("keyLen", keyLen, "keyPrefix", keyPrefix, "sender", sender,
                          "brevoStatus", e.getStatusCode().value(),
                          "brevoBody", e.getResponseBodyAsString());
        } catch (Exception e) {
            return Map.of("keyLen", keyLen, "keyPrefix", keyPrefix, "sender", sender,
                          "error", String.valueOf(e.getMessage()));
        }
    }
}
