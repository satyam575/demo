package com.example.demo.repositories;

import com.example.demo.models.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {
    
    boolean existsByPostIdAndMemberId(UUID postId, UUID memberId);
    
    @Query("SELECT pl.postId as postId, COUNT(pl) as likeCount FROM PostLike pl WHERE pl.postId IN :postIds GROUP BY pl.postId")
    List<PostLikeCount> countByPostIdIn(@Param("postIds") List<UUID> postIds);
    
    List<PostLike> findPostIdsByMemberIdAndPostIdIn(UUID memberId, List<UUID> postIds);
    
    Optional<PostLike> findByPostIdAndMemberId(UUID postId, UUID memberId);
    
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.postId = :postId")
    long countByPostId(@Param("postId") UUID postId);
    
    @Query("SELECT pl FROM PostLike pl WHERE pl.postId = :postId ORDER BY pl.createdAt DESC")
    List<PostLike> findByPostIdOrderByCreatedAtDesc(@Param("postId") UUID postId);
    
    void deleteByPostIdAndMemberId(UUID postId, UUID memberId);
}
