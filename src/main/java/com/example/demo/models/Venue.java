package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "venues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "wedding_id", nullable = false)
    private UUID weddingId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "maps_url")
    private String mapsUrl;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
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
    
    // Constructor for creating new venues
    public Venue(UUID weddingId, String name, String address, String mapsUrl, String notes) {
        this.weddingId = weddingId;
        this.name = name;
        this.address = address;
        this.mapsUrl = mapsUrl;
        this.notes = notes;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateDetails(String name, String address, String mapsUrl, String notes) {
        this.name = name;
        this.address = address;
        this.mapsUrl = mapsUrl;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }
}
