package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "wedding_id", nullable = false)
    private UUID weddingId;
    
    @Column(name = "venue_id")
    private UUID venueId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    
    @Column(name = "end_time", nullable = false)
    private Instant endTime;
    
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
    
    // Constructor for creating new events
    public Event(UUID weddingId, UUID venueId, String title, String description, 
                Instant startTime, Instant endTime) {
        this.weddingId = weddingId;
        this.venueId = venueId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateDetails(String title, String description, Instant startTime, Instant endTime) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.updatedAt = Instant.now();
    }
    
    public void setVenue(UUID venueId) {
        this.venueId = venueId;
        this.updatedAt = Instant.now();
    }
}
