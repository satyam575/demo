package com.example.demo.controllers;

import com.example.demo.auth.dtos.*;
import com.example.demo.auth.dtos.WeddingMemberDto;
import com.example.demo.auth.jwt.JwtIssuer;
import com.example.demo.models.MediaType;
import com.example.demo.models.MemberStatus;
import com.example.demo.models.WeddingMember;
import com.example.demo.services.PostService;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WeddingMemberRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {
    
    private final PostService postService;
    private final JwtIssuer jwtIssuer;
    private final WeddingMemberRepository weddingMemberRepository;
    private final UserRepository userRepository;
    
    @PostMapping("/weddings/{weddingId}/posts")
    public ResponseEntity<?> createPost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @Valid @RequestBody CreatePostDto request) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Create post
            PostDto response = postService.createPost(
                    UUID.fromString(userId),
                    request
            );
            
            log.info("User {} created post in wedding {}", userId, weddingId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid post creation request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("POST_CREATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in post creation", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/weddings/{weddingId}/members")
    public ResponseEntity<?> getWeddingMembers(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            var members = weddingMemberRepository.findByWeddingIdAndStatus(
                    java.util.UUID.fromString(weddingId), MemberStatus.ACCEPTED);

            java.util.List<WeddingMemberDto> result = new java.util.ArrayList<>();
            for (WeddingMember wm : members) {
                var userOpt = userRepository.findById(wm.getUserId());
                String name = userOpt.map(u -> u.getName()).orElse(null);
                String avatar = userOpt.map(u -> u.getAvatarUrl()).orElse(null);
                result.add(new WeddingMemberDto(
                        wm.getId(), wm.getUserId(), name, wm.getDisplayName(),
                        wm.getRole(), wm.getStatus(), avatar, wm.getJoinedAt()
                ));
            }

            log.info("Retrieved {} members for wedding {} by user {}", result.size(), weddingId, userId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Unexpected error in getting wedding members", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/weddings/{weddingId}/posts")
    public ResponseEntity<?> getWeddingPosts(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Get posts
            Page<PostDto> posts = postService.getWeddingPosts(
                    UUID.fromString(userId),
                    UUID.fromString(weddingId),
                    page,
                    size
            );
            
            log.info("Retrieved {} posts for wedding {} by user {}", posts.getTotalElements(), weddingId, userId);
            return ResponseEntity.ok(posts);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for wedding posts: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("POSTS_RETRIEVAL_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in getting wedding posts", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String postId,
            @Valid @RequestBody CreateCommentDto request) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Create comment
            CommentDto response = postService.createComment(
                    UUID.fromString(userId),
                    request
            );
            
            log.info("User {} created comment on post {}", userId, postId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid comment creation request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("COMMENT_CREATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in comment creation", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> getPostComments(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Get comments
            Page<CommentDto> comments = postService.getPostComments(
                    UUID.fromString(userId),
                    UUID.fromString(postId),
                    page,
                    size
            );
            
            log.info("Retrieved {} comments for post {} by user {}", comments.getTotalElements(), postId, userId);
            return ResponseEntity.ok(comments);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for post comments: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("COMMENTS_RETRIEVAL_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in getting post comments", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> togglePostLike(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String postId) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Toggle like
            postService.togglePostLike(
                    UUID.fromString(userId),
                    UUID.fromString(postId)
            );
            
            log.info("User {} toggled like on post {}", userId, postId);
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid like request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("LIKE_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in toggling like", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/weddings/{weddingId}/media/presigned-url")
    public ResponseEntity<?> generatePresignedUploadUrl(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @Valid @RequestBody PresignedUploadRequestDto request) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Generate presigned upload URL
            PresignedUploadResponse response = postService.generatePresignedUploadUrl(
                    UUID.fromString(userId),
                    UUID.fromString(weddingId),
                    request.getMediaType(),
                    request.getContentType()
            );
            
            log.info("Generated presigned upload URL for wedding {} by user {}", weddingId, userId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid presigned URL request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("PRESIGNED_URL_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in generating presigned URL", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/weddings/{weddingId}/media/upload")
    public ResponseEntity<?> uploadMedia(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @RequestParam("file") MultipartFile file) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Upload media (fallback method)
            String mediaUrl = postService.uploadMedia(
                    UUID.fromString(userId),
                    UUID.fromString(weddingId),
                    file
            );
            
            log.info("Uploaded media for wedding {} by user {}", weddingId, userId);
            return ResponseEntity.ok(new java.util.HashMap<String, String>() {{
                put("mediaUrl", mediaUrl);
            }});
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid media upload request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("MEDIA_UPLOAD_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in media upload", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/weddings/{weddingId}/members/{memberId}")
    public ResponseEntity<?> getMemberProfile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @PathVariable String memberId) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            java.util.UUID wId = java.util.UUID.fromString(weddingId);
            java.util.UUID mId = java.util.UUID.fromString(memberId);

            var memberOpt = weddingMemberRepository.findById(mId);
            if (memberOpt.isEmpty() || !memberOpt.get().getWeddingId().equals(wId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Member not found"));
            }
            var member = memberOpt.get();
            var userOpt = userRepository.findById(member.getUserId());
            String name = userOpt.map(u -> u.getName()).orElse(null);
            String avatar = userOpt.map(u -> u.getAvatarUrl()).orElse(null);
            long postCount = 0L;

            com.example.demo.auth.dtos.MemberProfileDto dto = new com.example.demo.auth.dtos.MemberProfileDto(
                    member.getId(), member.getWeddingId(), member.getUserId(),
                    name, member.getDisplayName(), avatar,
                    member.getRole(), member.getStatus(), member.getJoinedAt(),
                    postCount
            );
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    @GetMapping("/weddings/{weddingId}/members/{memberId}/posts")
    public ResponseEntity<?> getMemberPosts(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @PathVariable String memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            var posts = postService.getMemberPosts(
                    java.util.UUID.fromString(userId),
                    java.util.UUID.fromString(weddingId),
                    java.util.UUID.fromString(memberId),
                    page, size);
            return ResponseEntity.ok(posts);
        } catch (IllegalArgumentException e) {
            ErrorResponseDto error = new ErrorResponseDto("POSTS_RETRIEVAL_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

