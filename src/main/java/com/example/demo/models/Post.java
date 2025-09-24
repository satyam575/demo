package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "wedding_id", nullable = false)
    private UUID weddingId;
    
    @Column(name = "author_member_id", nullable = false)
    private UUID authorMemberId;
    
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility = PostVisibility.PUBLIC;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
    @Column(name = "media_count", nullable = false)
    private int mediaCount = 0;
    
    @Column(name = "primary_media_id")
    private UUID primaryMediaId;
    
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
    
    // Constructor for creating new posts
    public Post(UUID weddingId, UUID authorMemberId, String contentText, PostVisibility visibility) {
        this.weddingId = weddingId;
        this.authorMemberId = authorMemberId;
        this.contentText = contentText;
        this.visibility = visibility;
        this.isDeleted = false;
        this.mediaCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void markAsDeleted() {
        this.isDeleted = true;
        this.updatedAt = Instant.now();
    }
    
    public void updateContent(String contentText) {
        this.contentText = contentText;
        this.updatedAt = Instant.now();
    }
    
    public void setPrimaryMedia(UUID mediaId) {
        this.primaryMediaId = mediaId;
        this.updatedAt = Instant.now();
    }
    
    public void incrementMediaCount() {
        this.mediaCount++;
        this.updatedAt = Instant.now();
    }
}
