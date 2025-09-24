package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadResponse {
    
    private String uploadUrl;
    private String objectKey;
    private String mediaUrl;
    private Instant expiration;
}
