package com.example.demo.repositories;

import com.example.demo.models.Helpline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HelplineRepository extends JpaRepository<Helpline, UUID> {
    List<Helpline> findByWeddingIdOrderByCreatedAtDesc(UUID weddingId);
}

