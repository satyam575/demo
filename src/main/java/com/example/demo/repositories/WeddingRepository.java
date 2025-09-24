package com.example.demo.repositories;

import com.example.demo.models.Wedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeddingRepository extends JpaRepository<Wedding, UUID> {
    
    Optional<Wedding> findByCode(String code);
    
    Optional<Wedding> findByCodeAndIsActive(String code, boolean isActive);
    
    boolean existsByCode(String code);
}
