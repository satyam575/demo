package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestResponseDto {
    private String requestId;
    private int expiresIn; // in seconds
    private int retryAfter; // in seconds
}
