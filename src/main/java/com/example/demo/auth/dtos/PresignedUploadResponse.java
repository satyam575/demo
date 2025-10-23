package com.example.demo.auth.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class PresignedUploadResponse {
    
    private String uploadUrl;
    private String objectKey;
    private String mediaUrl;
    private Instant expiration;
    // Extra metadata to ensure the browser sends exactly what was signed
    private String contentType;
    private String cacheControl;
}
