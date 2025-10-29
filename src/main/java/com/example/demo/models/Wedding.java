package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wedding {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String code;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "partner1")
    private String partner1;
    
    @Column(name = "partner2")
    private String partner2;
    
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    // Constructor for creating new weddings
    public Wedding(String code, String title, String partner1, String partner2) {
        this.code = code;
        this.title = title;
        this.partner1 = partner1;
        this.partner2 = partner2;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Constructor for creating new weddings with cover image
    public Wedding(String code, String title, String partner1, String partner2, String coverImageUrl) {
        this.code = code;
        this.title = title;
        this.partner1 = partner1;
        this.partner2 = partner2;
        this.coverImageUrl = coverImageUrl;
        this.isActive = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
