package com.example.demo.repositories;

import com.example.demo.models.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    List<PushSubscription> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<PushSubscription> findByUserIdAndEndpoint(UUID userId, String endpoint);
}

