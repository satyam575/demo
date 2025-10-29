package com.example.demo.repositories;

import com.example.demo.models.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    List<Challenge> findByWeddingIdAndActiveTrueOrderByCreatedAtDesc(UUID weddingId);
    List<Challenge> findByWeddingIdAndEventIdAndActiveTrueOrderByCreatedAtDesc(UUID weddingId, UUID eventId);
    Optional<Challenge> findByWeddingIdAndEventIdAndTag(UUID weddingId, UUID eventId, String tag);
    Optional<Challenge> findByWeddingIdAndTagAndEventIdIsNull(UUID weddingId, String tag);
}

