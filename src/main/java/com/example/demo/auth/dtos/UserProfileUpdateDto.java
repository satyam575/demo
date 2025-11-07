package com.example.demo.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateDto {
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "Username must be 3-20 characters, alphanumeric and underscores only")
    private String username;
    
    // Allow any http/https URL; uploads are handled via dedicated endpoint
    @Pattern(regexp = "^https?://.+$", message = "Invalid avatar URL format")
    private String avatarUrl;
}
