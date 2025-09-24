package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageProvider storage = StorageProvider.R2;
    
    @Column(name = "object_key", nullable = false)
    private String objectKey;
    
    @Column(name = "mime_type", nullable = false)
    private String mimeType;
    
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    
    @Column(name = "duration_sec")
    private Integer durationSec;
    
    @Column(name = "order_index", nullable = false)
    private int orderIndex;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transcode_status")
    private TranscodeStatus transcodeStatus;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    // Constructor for creating new post media
    public PostMedia(UUID postId, MediaType type, String objectKey, String mimeType, 
                    long sizeBytes, int orderIndex) {
        this.postId = postId;
        this.type = type;
        this.objectKey = objectKey;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.orderIndex = orderIndex;
        this.storage = StorageProvider.R2;
        this.transcodeStatus = type == MediaType.VIDEO ? TranscodeStatus.PENDING : null;
        this.createdAt = Instant.now();
    }
    
    public void setTranscodeStatus(TranscodeStatus status) {
        this.transcodeStatus = status;
    }
    
    public void setDuration(Integer durationSec) {
        this.durationSec = durationSec;
    }
}
