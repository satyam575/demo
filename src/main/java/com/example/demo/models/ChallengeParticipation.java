package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "challenge_participations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"challenge_id", "post_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "user_id")
    private UUID userId; // optional, for attribution

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

