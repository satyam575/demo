package com.example.demo.auth.vendor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Msg91OtpRequest {
    @JsonProperty("widgetId")
    private String widgetId;
    
    @JsonProperty("identifier")
    private String identifier;
}
