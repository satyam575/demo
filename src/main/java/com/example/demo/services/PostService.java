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
import java.util.Map;
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
    private final com.example.demo.config.AwsS3Properties awsS3Properties;
    private final com.example.demo.repositories.PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushService webPushService;
    
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
                log.info("Processing media URL: {}", mediaUrl);
                // Extract object key from URL, store only the leaf (without weddingId prefix)
                String objectKeyWithPrefix = mediaUrl.replace(awsS3Properties.getPublicUrl() + "/", "");
                String objectKey;
                int lastSlash = objectKeyWithPrefix.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < objectKeyWithPrefix.length() - 1) {
                    objectKey = objectKeyWithPrefix.substring(lastSlash + 1);
                } else {
                    objectKey = objectKeyWithPrefix;
                }
                log.info("Extracted leaf object key: {} (from: {})", objectKey, objectKeyWithPrefix);
                
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

        // Notify all accepted wedding members about the new post (excluding author)
        try {
            var members = weddingMemberRepository.findByWeddingIdAndStatus(weddingId, MemberStatus.ACCEPTED);
            String weddingTitle = weddingService.getWeddingMember(userId, weddingId)
                    .map(m -> m.getWedding().getTitle())
                    .orElse("Wedding");

            String authorName = userRepository.findById(member.getUserId())
                    .map(u -> u.getName() != null ? u.getName() : "Someone")
                    .orElse("Someone");

            String url = "/wedding/" + weddingId;
            String title = "New post in " + weddingTitle;
            String body = authorName + " shared a memory";
            String json = String.format("{\"type\":\"NEW_POST\",\"title\":%s,\"body\":%s,\"url\":%s,\"weddingId\":%s,\"postId\":%s}",
                    toJsonString(title), toJsonString(body), toJsonString(url),
                    toJsonString(weddingId.toString()), toJsonString(savedPost.getId().toString()));

            for (WeddingMember wm : members) {
                if (wm.getUserId().equals(userId)) continue;
                var subs = pushSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(wm.getUserId());
                subs.forEach(s -> webPushService.send(s.getEndpoint(), s.getP256dh(), s.getAuth(), json));
            }
        } catch (Exception e) {
            log.warn("Failed to dispatch web push for new post {}: {}", savedPost.getId(), e.getMessage());
        }

        return convertToPostDto(savedPost, userId);
    }
    
    @Transactional(readOnly = true)
    public Page<PostDto> getWeddingPosts(UUID userId, UUID weddingId, int page, int size) {
        // Verify user is member of the wedding
        if (!weddingService.isUserMemberOfWedding(userId, weddingId)) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = postRepository.findByWeddingIdAndIsDeletedFalseOrderByCreatedAtDesc(weddingId, pageable);
        
        // Batch load all related data to avoid N+1 queries
        List<PostDto> optimizedPostDtos = convertToPostDtoBatch(posts.getContent(), userId, weddingId);
        
        return posts.map(post -> {
            // Find the optimized DTO for this post
            return optimizedPostDtos.stream()
                    .filter(dto -> dto.getId().equals(post.getId()))
                    .findFirst()
                    .orElse(convertToPostDto(post, userId)); // Fallback to original method
        });
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
        // increment denormalized comment count
        postRepository.incrementCommentCount(postId);
        
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
            postRepository.decrementLikeCount(postId);
            log.info("User {} unliked post {}", userId, postId);
        } else {
            // Like
            PostLike like = new PostLike(postId, member.getId());
            postLikeRepository.save(like);
            postRepository.incrementLikeCount(postId);
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
    
    /**
     * Optimized method to convert multiple posts to DTOs with batch queries
     */
    private List<PostDto> convertToPostDtoBatch(List<Post> posts, UUID userId, UUID weddingId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        
        // Extract all post IDs and author member IDs
        List<UUID> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        List<UUID> authorMemberIds = posts.stream().map(Post::getAuthorMemberId).distinct().collect(Collectors.toList());
        
        // Batch load all author members
        List<WeddingMember> authorMembers = weddingMemberRepository.findAllById(authorMemberIds);
        Map<UUID, WeddingMember> authorMemberMap = authorMembers.stream()
                .collect(Collectors.toMap(WeddingMember::getId, member -> member));
        
        // Batch load all author users
        List<UUID> userIds = authorMembers.stream().map(WeddingMember::getUserId).collect(Collectors.toList());
        List<User> authorUsers = userRepository.findAllById(userIds);
        Map<UUID, User> userMap = authorUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        // Batch load all media for all posts
        List<PostMedia> allMedia = postMediaRepository.findByPostIdInOrderByPostIdAscOrderIndexAsc(postIds);
        Map<UUID, List<PostMedia>> mediaMap = allMedia.stream()
                .collect(Collectors.groupingBy(PostMedia::getPostId));
        
        // Counts now come from the denormalized columns on Post
        
        // Get user member once for like checking
        Optional<WeddingMember> userMember = weddingService.getWeddingMember(userId, weddingId);
        Map<UUID, Boolean> likedByUserMap; // Will be populated if user member exists
        
        if (userMember.isPresent()) {
            List<PostLike> likedPostIds = postLikeRepository.findPostIdsByMemberIdAndPostIdIn(userMember.get().getId(), postIds);
            likedByUserMap = likedPostIds.stream()
                    .collect(Collectors.toMap(PostLike::getPostId, postId -> true));
        } else {
            likedByUserMap = Map.of();
        }

        // Convert posts to DTOs using batch-loaded data
        return posts.stream().map(post -> {
            WeddingMember authorMember = authorMemberMap.get(post.getAuthorMemberId());
            User authorUser = userMap.get(authorMember.getUserId());
            List<PostMedia> mediaList = mediaMap.getOrDefault(post.getId(), List.of());
            List<PostMediaDto> mediaDtos = mediaList.stream()
                    .map(media -> convertToPostMediaDto(media, weddingId))
                    .collect(Collectors.toList());
            
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
                    post.getLikeCount(),
                    post.getCommentCount(),
                    likedByUserMap.getOrDefault(post.getId(), false),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }).collect(Collectors.toList());
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
                .map(media -> convertToPostMediaDto(media, post.getWeddingId()))
                .collect(Collectors.toList());
        
        // Use denormalized counts on Post
        long likeCount = post.getLikeCount();
        long commentCount = post.getCommentCount();
        
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
    
    private PostMediaDto convertToPostMediaDto(PostMedia media, UUID weddingId) {
        // We store only the leaf object key (e.g., "uuid.jpg").
        // When returning, prepend the weddingId folder. For backward compatibility,
        // if the stored key already contains the weddingId prefix, don't add it again.
        String storedKey = media.getObjectKey();
        String prefix = weddingId.toString() + "/";
        String fullObjectKey = storedKey.startsWith(prefix) ? storedKey : (prefix + storedKey);
        String mediaUrl = awsS3Properties.getPublicUrl() + "/" + fullObjectKey;
        return new PostMediaDto(
                media.getId(),
                media.getType(),
                mediaUrl,
                media.getMimeType(),
                media.getSizeBytes(),
                media.getDurationSec(),
                media.getOrderIndex(),
                media.getTranscodeStatus(),
                media.getCreatedAt()
        );
    }

    private String toJsonString(String v) {
        return '"' + v.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
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

    public org.springframework.data.domain.Page<PostDto> getMemberPosts(java.util.UUID userId, java.util.UUID weddingId, java.util.UUID authorMemberId, int page, int size) {
        if (!weddingService.isUserMemberOfWedding(userId, weddingId)) {
            throw new IllegalArgumentException("User is not a member of this wedding");
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.example.demo.models.Post> posts = postRepository.findByAuthorMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(authorMemberId, pageable);
        java.util.List<PostDto> optimized = convertToPostDtoBatch(posts.getContent(), userId, weddingId);
        return posts.map(post -> optimized.stream()
                .filter(dto -> dto.getId().equals(post.getId()))
                .findFirst()
                .orElse(convertToPostDto(post, userId)));
    }
}

