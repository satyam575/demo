package com.example.demo.services;

import com.example.demo.config.WebPushProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.Security;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {

    private final WebPushProperties webPushProperties;

    private PushService getPushService() throws GeneralSecurityException {
        String publicKey = webPushProperties.getPublicKey();
        String privateKey = webPushProperties.getPrivateKey();
        if (publicKey == null || publicKey.isBlank() || privateKey == null || privateKey.isBlank()) {
            throw new GeneralSecurityException("Missing VAPID keys: ensure webpush.publicKey and webpush.privateKey are configured (profile/env)");
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        PushService pushService = new PushService();
        pushService.setSubject(webPushProperties.getSubject());
        pushService.setPublicKey(Utils.loadPublicKey(publicKey));
        pushService.setPrivateKey(Utils.loadPrivateKey(privateKey));
        return pushService;
    }

    public void send(String endpoint, String p256dh, String auth, String jsonPayload) {
        try {
            Notification notification = new Notification(endpoint, p256dh, auth, jsonPayload);
            getPushService().send(notification);
        } catch (Exception e) {
            log.error("Failed to send web push", e);
        }
    }

    @PostConstruct
    public void logConfigPresence() {
        boolean hasPub = webPushProperties.getPublicKey() != null && !webPushProperties.getPublicKey().isBlank();
        boolean hasPriv = webPushProperties.getPrivateKey() != null && !webPushProperties.getPrivateKey().isBlank();
        log.info("WebPush configured: publicKey={}, privateKey={}, subjectPresent={}", hasPub, hasPriv, webPushProperties.getSubject() != null);
    }
}
