package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_likes", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "member_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    
    @Column(name = "member_id", nullable = false)
    private UUID memberId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    // Constructor for creating new post likes
    public PostLike(UUID postId, UUID memberId) {
        this.postId = postId;
        this.memberId = memberId;
        this.createdAt = Instant.now();
    }
}
