package com.example.demo.auth.vendor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Msg91OtpVerifyRequest {
    @JsonProperty("widgetId")
    private String widgetId;
    
    @JsonProperty("reqId")
    private String reqId;
    
    @JsonProperty("otp")
    private String otp;
}
