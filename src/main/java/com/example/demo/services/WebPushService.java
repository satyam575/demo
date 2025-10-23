package com.example.demo.services;

import com.example.demo.config.WebPushProperties;
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
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        PushService pushService = new PushService();
        pushService.setSubject(webPushProperties.getSubject());
        pushService.setPublicKey(Utils.loadPublicKey(webPushProperties.getPublicKey()));
        pushService.setPrivateKey(Utils.loadPrivateKey(webPushProperties.getPrivateKey()));
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
}

