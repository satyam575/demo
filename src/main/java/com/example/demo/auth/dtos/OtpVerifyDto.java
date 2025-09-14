package com.example.demo.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerifyDto {
    @NotBlank(message = "Request ID is required")
    private String requestId;
    
    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{4,8}$", message = "OTP code must be 4-8 digits")
    private String code;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}
