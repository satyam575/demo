package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "webpush")
public class WebPushProperties {
    private String publicKey;
    private String privateKey;
    private String subject = "mailto:support@example.com";
}

