package com.example.demo.repositories;

import com.example.demo.models.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    Page<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID postId, Pageable pageable);
    
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID postId);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId AND c.isDeleted = false")
    long countByPostIdAndIsDeletedFalse(@Param("postId") UUID postId);
    
    @Query("SELECT c.postId as postId, COUNT(c) as commentCount FROM Comment c WHERE c.postId IN :postIds AND c.isDeleted = false GROUP BY c.postId")
    List<PostCommentCount> countByPostIdInAndIsDeletedFalse(@Param("postIds") List<UUID> postIds);
    
    @Query("SELECT c FROM Comment c WHERE c.authorMemberId = :authorMemberId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    Page<Comment> findByAuthorMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID authorMemberId, Pageable pageable);
}
