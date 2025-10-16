package com.example.demo.repositories;

import com.example.demo.models.PostMedia;
import com.example.demo.models.TranscodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {
    
    List<PostMedia> findByPostIdOrderByOrderIndex(UUID postId);
    
    List<PostMedia> findByPostIdInOrderByPostIdAscOrderIndexAsc(List<UUID> postIds);
    
    @Query("SELECT pm FROM PostMedia pm WHERE pm.postId = :postId ORDER BY pm.orderIndex")
    List<PostMedia> findByPostIdOrderByOrderIndexAsc(@Param("postId") UUID postId);
    
    @Query("SELECT pm FROM PostMedia pm WHERE pm.transcodeStatus = :status")
    List<PostMedia> findByTranscodeStatus(@Param("status") TranscodeStatus status);
    
    @Query("SELECT COUNT(pm) FROM PostMedia pm WHERE pm.postId = :postId")
    long countByPostId(@Param("postId") UUID postId);
    
    void deleteByPostId(UUID postId);
}
