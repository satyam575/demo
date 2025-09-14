package com.example.demo.auth.vendor;

import lombok.Data;

@Data
public class Msg91OtpResponse {
    private String type;
    private String message; // This contains the request ID for MSG91 OTP Widget API
    
    // Helper method to get request ID from message field
    public String getRequestId() {
        return message;
    }
}
