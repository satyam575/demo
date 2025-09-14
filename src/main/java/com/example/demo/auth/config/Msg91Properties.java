package com.example.demo.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "msg91")
public class Msg91Properties {
    private String baseUrl;
    private String authKey;
    private String widgetId;
    private Otp otp = new Otp();

    @Data
    public static class Otp {
        private String generatePath;
        private String verifyPath;
        private String templateId;
    }
}
