package com.example.demo.services;

import com.example.demo.auth.dtos.*;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    
    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final WeddingMemberRepository weddingMemberRepository;
    private final UserRepository userRepository;
    private final WeddingService weddingService;
    private final S3Service s3Service;
    
    @Transactional
    public PostDto createPost(UUID userId, CreatePostDto request) {
        // Verify user is member of the wedding
        UUID weddingId = UUID.fromString(request.getWeddingId());
        if (!weddingService.isUserMemberOfWedding(userId, weddingId)) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        // Get wedding member
        WeddingMember member = weddingService.getWeddingMember(userId, weddingId)
                .orElseThrow(() -> new IllegalArgumentException("Wedding member not found"));
        
        // Create post
        Post post = new Post(weddingId, member.getId(), request.getContentText(), request.getVisibility());
        Post savedPost = postRepository.save(post);
        
        // Handle media if provided
        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            for (int i = 0; i < request.getMediaUrls().size(); i++) {
                String mediaUrl = request.getMediaUrls().get(i);
                // Extract object key from URL (assuming it's the last part after the last slash)
                String objectKey = mediaUrl.substring(mediaUrl.lastIndexOf("/") + 1);
                
                // Determine media type from URL or content type
                MediaType mediaType = determineMediaTypeFromUrl(mediaUrl);
                
                PostMedia postMedia = new PostMedia(
                        savedPost.getId(),
                        mediaType,
                        objectKey,
                        getMimeTypeFromUrl(mediaUrl),
                        0, // Size will be updated later
                        i
                );
                postMediaRepository.save(postMedia);
            }
            savedPost.setMediaCount(request.getMediaUrls().size());
            postRepository.save(savedPost);
        }
        
        log.info("Created post {} for wedding {} by user {}", savedPost.getId(), weddingId, userId);
        return convertToPostDto(savedPost, userId);
    }
    
    public Page<PostDto> getWeddingPosts(UUID userId, UUID weddingId, int page, int size) {
        // Verify user is member of the wedding
        if (!weddingService.isUserMemberOfWedding(userId, weddingId)) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByWeddingIdAndIsDeletedFalseOrderByCreatedAtDesc(weddingId, pageable);
        
        return posts.map(post -> convertToPostDto(post, userId));
    }
    
    @Transactional
    public CommentDto createComment(UUID userId, CreateCommentDto request) {
        UUID postId = UUID.fromString(request.getPostId());
        
        // Get post and verify user has access
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (!weddingService.isUserMemberOfWedding(userId, post.getWeddingId())) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        // Get wedding member
        WeddingMember member = weddingService.getWeddingMember(userId, post.getWeddingId())
                .orElseThrow(() -> new IllegalArgumentException("Wedding member not found"));
        
        // Create comment
        Comment comment = new Comment(postId, member.getId(), request.getContentText());
        Comment savedComment = commentRepository.save(comment);
        
        log.info("Created comment {} for post {} by user {}", savedComment.getId(), postId, userId);
        return convertToCommentDto(savedComment);
    }
    
    public Page<CommentDto> getPostComments(UUID userId, UUID postId, int page, int size) {
        // Get post and verify user has access
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (!weddingService.isUserMemberOfWedding(userId, post.getWeddingId())) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId, pageable);
        
        return comments.map(this::convertToCommentDto);
    }
    
    @Transactional
    public void togglePostLike(UUID userId, UUID postId) {
        // Get post and verify user has access
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        
        if (!weddingService.isUserMemberOfWedding(userId, post.getWeddingId())) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        // Get wedding member
        WeddingMember member = weddingService.getWeddingMember(userId, post.getWeddingId())
                .orElseThrow(() -> new IllegalArgumentException("Wedding member not found"));
        
        // Check if already liked
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, member.getId());
        
        if (existingLike.isPresent()) {
            // Unlike
            postLikeRepository.delete(existingLike.get());
            log.info("User {} unliked post {}", userId, postId);
        } else {
            // Like
            PostLike like = new PostLike(postId, member.getId());
            postLikeRepository.save(like);
            log.info("User {} liked post {}", userId, postId);
        }
    }
    
    public com.example.demo.auth.dtos.PresignedUploadResponse generatePresignedUploadUrl(UUID userId, UUID weddingId, 
            com.example.demo.models.MediaType mediaType, String contentType) {
        // Verify user is member of the wedding
        if (!weddingService.isUserMemberOfWedding(userId, weddingId)) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        // Generate presigned upload URL
        com.example.demo.auth.dtos.PresignedUploadResponse response = s3Service.generatePresignedUploadUrl(
                mediaType, weddingId, contentType, 0L);
        
        log.info("Generated presigned upload URL for wedding {} by user {}", weddingId, userId);
        return response;
    }
    
    public String uploadMedia(UUID userId, UUID weddingId, MultipartFile file) throws IOException {
        // Verify user is member of the wedding
        if (!weddingService.isUserMemberOfWedding(userId, weddingId)) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        // Determine media type
        MediaType mediaType = determineMediaTypeFromContentType(file.getContentType());
        
        // Upload to S3
        String objectKey = s3Service.uploadMedia(file, mediaType, weddingId);
        String mediaUrl = s3Service.getMediaUrl(objectKey);
        
        log.info("Uploaded media {} for wedding {} by user {}", objectKey, weddingId, userId);
        return mediaUrl;
    }
    
    private PostDto convertToPostDto(Post post, UUID userId) {
        // Get author info
        WeddingMember authorMember = weddingMemberRepository.findById(post.getAuthorMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Author member not found"));
        
        User authorUser = userRepository.findById(authorMember.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Author user not found"));
        
        // Get media
        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByOrderIndex(post.getId());
        List<PostMediaDto> mediaDtos = mediaList.stream()
                .map(this::convertToPostMediaDto)
                .collect(Collectors.toList());
        
        // Get like count
        long likeCount = postLikeRepository.countByPostId(post.getId());
        
        // Get comment count
        long commentCount = commentRepository.countByPostIdAndIsDeletedFalse(post.getId());
        
        // Check if user liked this post
        boolean isLikedByUser = false;
        if (userId != null) {
            Optional<WeddingMember> userMember = weddingService.getWeddingMember(userId, post.getWeddingId());
            if (userMember.isPresent()) {
                isLikedByUser = postLikeRepository.existsByPostIdAndMemberId(post.getId(), userMember.get().getId());
            }
        }
        
        return new PostDto(
                post.getId(),
                post.getWeddingId(),
                post.getAuthorMemberId(),
                authorUser.getName(),
                authorMember.getDisplayName(),
                post.getContentText(),
                post.getVisibility(),
                post.getMediaCount(),
                mediaDtos,
                (int) likeCount,
                (int) commentCount,
                isLikedByUser,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
    
    private PostMediaDto convertToPostMediaDto(PostMedia media) {
        return new PostMediaDto(
                media.getId(),
                media.getType(),
                s3Service.getMediaUrl(media.getObjectKey()),
                media.getMimeType(),
                media.getSizeBytes(),
                media.getDurationSec(),
                media.getOrderIndex(),
                media.getTranscodeStatus(),
                media.getCreatedAt()
        );
    }
    
    private CommentDto convertToCommentDto(Comment comment) {
        // Get author info
        WeddingMember authorMember = weddingMemberRepository.findById(comment.getAuthorMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Author member not found"));
        
        User authorUser = userRepository.findById(authorMember.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Author user not found"));
        
        return new CommentDto(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorMemberId(),
                authorUser.getName(),
                authorMember.getDisplayName(),
                comment.getContentText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
    
    private MediaType determineMediaTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains("image") || lowerUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return MediaType.IMAGE;
        } else if (lowerUrl.contains("video") || lowerUrl.matches(".*\\.(mp4|mov|avi)$")) {
            return MediaType.VIDEO;
        } else if (lowerUrl.contains("audio") || lowerUrl.matches(".*\\.(mp3|wav|ogg)$")) {
            return MediaType.AUDIO;
        }
        return MediaType.IMAGE; // Default
    }
    
    private MediaType determineMediaTypeFromContentType(String contentType) {
        if (contentType == null) return MediaType.IMAGE;
        
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        return MediaType.IMAGE; // Default
    }
    
    private String getMimeTypeFromUrl(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerUrl.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerUrl.endsWith(".mov")) {
            return "video/quicktime";
        } else if (lowerUrl.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (lowerUrl.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerUrl.endsWith(".wav")) {
            return "audio/wav";
        } else if (lowerUrl.endsWith(".ogg")) {
            return "audio/ogg";
        }
        return "application/octet-stream"; // Default
    }
}