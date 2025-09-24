package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    
    @Column(name = "author_member_id", nullable = false)
    private UUID authorMemberId;
    
    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;
    
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
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
    
    // Constructor for creating new comments
    public Comment(UUID postId, UUID authorMemberId, String contentText) {
        this.postId = postId;
        this.authorMemberId = authorMemberId;
        this.contentText = contentText;
        this.isDeleted = false;
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
}
