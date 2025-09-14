package com.example.demo.auth.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PhoneUtil {
    
    /**
     * Normalizes phone number by:
     * 1. Removing '+' prefix if present
     * 2. If 10 digits, prepend '91' (India country code)
     * 3. Return normalized format
     */
    public static String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        // Remove all non-digit characters except +
        String cleaned = phone.replaceAll("[^\\d+]", "");
        
        // Remove + prefix if present
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        
        // If 10 digits, assume it's Indian number and prepend 91
        if (cleaned.length() == 10) {
            cleaned = "91" + cleaned;
        }
        
        // Validate the final format
        if (!cleaned.matches("^[1-9]\\d{9,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format: " + phone);
        }
        
        log.debug("Normalized phone: {} -> {}", phone, cleaned);
        return cleaned;
    }
}
