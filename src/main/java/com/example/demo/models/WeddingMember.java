package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wedding_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"wedding_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeddingMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "wedding_id", nullable = false)
    private UUID weddingId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.PENDING;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wedding_id", insertable = false, updatable = false)
    private Wedding wedding;
    
    @PrePersist
    protected void onCreate() {
        joinedAt = Instant.now();
    }
    
    // Constructor for creating new wedding members
    public WeddingMember(UUID weddingId, UUID userId, MemberRole role, String displayName) {
        this.weddingId = weddingId;
        this.userId = userId;
        this.role = role;
        this.displayName = displayName;
        this.status = MemberStatus.PENDING;
        this.joinedAt = Instant.now();
    }
    
    // Constructor for creating new wedding members with status
    public WeddingMember(UUID weddingId, UUID userId, String displayName, MemberRole role, MemberStatus status) {
        this.weddingId = weddingId;
        this.userId = userId;
        this.role = role;
        this.displayName = displayName;
        this.status = status;
        this.joinedAt = Instant.now();
    }
    
    public void acceptInvitation() {
        this.status = MemberStatus.ACCEPTED;
    }
    
    public void declineInvitation() {
        this.status = MemberStatus.DECLINED;
    }
    
    public void blockMember() {
        this.status = MemberStatus.BLOCKED;
    }
}