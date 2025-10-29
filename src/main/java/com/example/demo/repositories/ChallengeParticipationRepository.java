package com.example.demo.repositories;

import com.example.demo.models.ChallengeParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, UUID> {
    long countByChallengeId(UUID challengeId);
    boolean existsByChallengeIdAndPostId(UUID challengeId, UUID postId);
}

