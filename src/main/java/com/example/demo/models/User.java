package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String username;
    
    @Column(unique = true)
    private String email;
    
    @Column(unique = true)
    private String phone;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;
    
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Constructor for creating new users
    public User(String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.phoneVerified = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    

    // Constructor for email-based OTP verification
    public User(String email) {
        this.name = "User"; // Default name, can be updated later
        this.email = email;
        this.phoneVerified = false;
        this.emailVerified = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void markPhoneVerified() {
        this.phoneVerified = true;
        this.updatedAt = Instant.now();
    }
    
    public void markEmailVerified() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }
    
    public void updateName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }
    
    public void updateEmail(String email) {
        this.email = email;
        this.updatedAt = Instant.now();
    }
    
    public void updateUsername(String username) {
        this.username = username;
        this.updatedAt = Instant.now();
    }
    
    public void updateAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        this.updatedAt = Instant.now();
    }
}
