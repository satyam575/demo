package com.example.demo.repositories;

import com.example.demo.models.MemberStatus;
import com.example.demo.models.WeddingMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeddingMemberRepository extends JpaRepository<WeddingMember, UUID> {
    
    List<WeddingMember> findByUserId(UUID userId);
    
    List<WeddingMember> findByWeddingId(UUID weddingId);
    
    Optional<WeddingMember> findByWeddingIdAndUserId(UUID weddingId, UUID userId);
    
    boolean existsByWeddingIdAndUserId(UUID weddingId, UUID userId);
    
    boolean existsByWeddingIdAndUserIdAndStatus(UUID weddingId, UUID userId, MemberStatus status);
    
    @Query("SELECT wm FROM WeddingMember wm WHERE wm.userId = :userId AND wm.status = :status")
    List<WeddingMember> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") MemberStatus status);
    
    @Query("SELECT wm FROM WeddingMember wm WHERE wm.weddingId = :weddingId AND wm.status = :status")
    List<WeddingMember> findByWeddingIdAndStatus(@Param("weddingId") UUID weddingId, @Param("status") MemberStatus status);
}