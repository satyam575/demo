package com.example.demo.repositories;

import com.example.demo.models.Post;
import com.example.demo.models.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    Page<Post> findByWeddingIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID weddingId, Pageable pageable);
    
    List<Post> findByWeddingIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID weddingId);
    
    @Query("SELECT p FROM Post p WHERE p.weddingId = :weddingId AND p.isDeleted = false AND p.visibility = :visibility ORDER BY p.createdAt DESC")
    Page<Post> findByWeddingIdAndVisibilityAndIsDeletedFalseOrderByCreatedAtDesc(
            @Param("weddingId") UUID weddingId, 
            @Param("visibility") PostVisibility visibility, 
            Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.authorMemberId = :authorMemberId AND p.isDeleted = false ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID authorMemberId, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.weddingId = :weddingId AND p.isDeleted = false")
    long countByWeddingIdAndIsDeletedFalse(@Param("weddingId") UUID weddingId);
}
