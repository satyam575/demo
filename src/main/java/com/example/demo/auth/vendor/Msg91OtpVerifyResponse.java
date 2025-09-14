package com.example.demo.auth.vendor;

import lombok.Data;

@Data
public class Msg91OtpVerifyResponse {
    private String type;
    private String message; // This contains the JWT token for successful verification
    
    // Helper method to check if verification was successful
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(type);
    }
    
    // Helper method to get the JWT token from message
    public String getToken() {
        return message;
    }
}
