package com.example.demo.controllers;

import com.example.demo.auth.dtos.ErrorResponseDto;
import com.example.demo.auth.jwt.JwtIssuer;
import com.example.demo.models.PushSubscription;
import com.example.demo.repositories.PushSubscriptionRepository;
import com.example.demo.services.WebPushService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationsController {

    private final JwtIssuer jwtIssuer;
    private final PushSubscriptionRepository repository;
    private final WebPushService webPushService;

    @Data
    public static class SubscriptionKeys { private String p256dh; private String auth; }
    @Data
    public static class SubscribeRequest { private String endpoint; private SubscriptionKeys keys; }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestHeader("Authorization") String authHeader,
                                       @Valid @RequestBody SubscribeRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = UUID.fromString(jwtIssuer.getUserIdFromToken(token));

            if (request.getEndpoint() == null || request.getKeys() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid subscription"));
            }

            var existing = repository.findByUserIdAndEndpoint(userId, request.getEndpoint());
            PushSubscription sub = existing.orElseGet(PushSubscription::new);
            if (sub.getId() == null) sub.setUserId(userId);
            sub.setEndpoint(request.getEndpoint());
            sub.setP256dh(request.getKeys().getP256dh());
            sub.setAuth(request.getKeys().getAuth());
            repository.save(sub);

            return ResponseEntity.ok(Map.of("status", "subscribed"));
        } catch (Exception e) {
            log.error("Failed to subscribe for push", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("SUBSCRIBE_ERROR", "Failed to save subscription"));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestHeader("Authorization") String authHeader,
                                  @RequestBody(required = false) Map<String, Object> payload) {
        try {
            String token = authHeader.replace("Bearer ", "");
            UUID userId = UUID.fromString(jwtIssuer.getUserIdFromToken(token));

            List<PushSubscription> subs = repository.findByUserIdOrderByCreatedAtDesc(userId);
            if (subs.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No subscription"));
            }
            String title = String.valueOf(payload != null ? payload.getOrDefault("title", "MandapMemoir") : "MandapMemoir");
            String body = String.valueOf(payload != null ? payload.getOrDefault("body", "Hello from Web Push!") : "Hello from Web Push!");
            String url = String.valueOf(payload != null ? payload.getOrDefault("url", "/dashboard") : "/dashboard");
            String json = String.format("{\"title\":%s,\"body\":%s,\"url\":%s}",
                    toJsonString(title), toJsonString(body), toJsonString(url));

            subs.forEach(s -> webPushService.send(s.getEndpoint(), s.getP256dh(), s.getAuth(), json));
            return ResponseEntity.ok(Map.of("sent", subs.size()));
        } catch (Exception e) {
            log.error("Failed to send test notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("NOTIFY_ERROR", "Failed to send test notification"));
        }
    }

    private String toJsonString(String v) {
        return '"' + v.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}

