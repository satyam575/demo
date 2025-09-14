package com.example.demo.auth.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class EmailUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    /**
     * Normalizes email address by:
     * 1. Converting to lowercase
     * 2. Trimming whitespace
     * 3. Validating format
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        // Trim and convert to lowercase
        String normalized = email.trim().toLowerCase();
        
        // Validate the email format
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        log.debug("Normalized email: {} -> {}", email, normalized);
        return normalized;
    }
    
    /**
     * Validates if the email format is correct
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        try {
            normalizeEmail(email);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
