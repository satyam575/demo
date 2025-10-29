package com.example.demo.controllers;

import com.example.demo.auth.dtos.*;
import com.example.demo.auth.dtos.WeddingMemberDto;
import com.example.demo.auth.dtos.HelplineDto;
import com.example.demo.repositories.EventRepository;
import com.example.demo.repositories.VenueRepository;
import com.example.demo.models.Event;
import com.example.demo.models.Venue;
import com.example.demo.auth.jwt.JwtIssuer;
import com.example.demo.models.MediaType;
import com.example.demo.models.MemberStatus;
import com.example.demo.models.WeddingMember;
import com.example.demo.services.PostService;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.WeddingMemberRepository;
import com.example.demo.repositories.WeddingRepository;
import com.example.demo.repositories.HelplineRepository;
import com.example.demo.models.Helpline;
import com.example.demo.services.ChallengeService;
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
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ChallengeService challengeService;
    private final WeddingRepository weddingRepository;
    private final HelplineRepository helplineRepository;
    
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

    @GetMapping("/weddings/{weddingId}/venues")
    public ResponseEntity<?> getVenues(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable String weddingId) {
        try {
            var venues = venueRepository.findByWeddingId(java.util.UUID.fromString(weddingId));
            java.util.List<VenueDto> list = new java.util.ArrayList<>();
            for (Venue v : venues) {
                list.add(new VenueDto(v.getId(), v.getWeddingId(), v.getName(), v.getAddress(), v.getMapsUrl(), v.getNotes(), v.getCreatedAt()));
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to fetch venues"));
        }
    }

    @PostMapping("/weddings/{weddingId}/venues")
    public ResponseEntity<?> createVenue(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable String weddingId,
                                         @Valid @RequestBody CreateVenueDto request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            java.util.UUID userId = java.util.UUID.fromString(jwtIssuer.getUserIdFromToken(token));
            java.util.UUID wId = java.util.UUID.fromString(weddingId);
            var memberOpt = weddingMemberRepository.findByWeddingIdAndUserId(wId, userId);
            if (memberOpt.isEmpty() || memberOpt.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            Venue v = new Venue(wId, request.getName(), request.getAddress(), request.getMapsUrl(), request.getNotes());
            var saved = venueRepository.save(v);
            return ResponseEntity.ok(new VenueDto(saved.getId(), saved.getWeddingId(), saved.getName(), saved.getAddress(), saved.getMapsUrl(), saved.getNotes(), saved.getCreatedAt()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to create venue"));
        }
    }

    @GetMapping("/weddings/{weddingId}/events")
    public ResponseEntity<?> getEvents(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable String weddingId) {
        try {
            var events = eventRepository.findByWeddingIdOrderByStartTimeAsc(java.util.UUID.fromString(weddingId));
            java.util.List<EventDto> list = new java.util.ArrayList<>();
            for (Event e : events) {
                list.add(new EventDto(e.getId(), e.getWeddingId(), e.getVenueId(), e.getTitle(), e.getDescription(), e.getStartTime()));
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to fetch events"));
        }
    }

    @PostMapping("/weddings/{weddingId}/events")
    public ResponseEntity<?> createEvent(@RequestHeader("Authorization") String authHeader,
                                         @PathVariable String weddingId,
                                         @Valid @RequestBody CreateEventDto request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            java.util.UUID userId = java.util.UUID.fromString(jwtIssuer.getUserIdFromToken(token));
            java.util.UUID wId = java.util.UUID.fromString(weddingId);
            var memberOpt = weddingMemberRepository.findByWeddingIdAndUserId(wId, userId);
            if (memberOpt.isEmpty() || memberOpt.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            java.util.UUID venueId = null;
            if (request.getVenueId() != null && !request.getVenueId().isBlank()) {
                venueId = java.util.UUID.fromString(request.getVenueId());
            }
            java.time.Instant start = java.time.Instant.parse(request.getStartTimeIso());
            java.time.Instant end;
            if (request.getEndTimeIso() != null && !request.getEndTimeIso().isBlank()) {
                end = java.time.Instant.parse(request.getEndTimeIso());
            } else {
                end = start.plus(java.time.Duration.ofHours(2));
            }
            Event e = new Event(wId, venueId, request.getTitle(), request.getDescription(), start, end);
            var saved = eventRepository.save(e);
            return ResponseEntity.ok(new EventDto(saved.getId(), saved.getWeddingId(), saved.getVenueId(), saved.getTitle(), saved.getDescription(), saved.getStartTime()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to create event"));
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

    // Challenges
    @GetMapping("/weddings/{weddingId}/challenges")
    public ResponseEntity<?> listChallenges(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @RequestParam(required = false) String eventId,
            @RequestParam(defaultValue = "true") boolean includeCounts,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        try {
            UUID wId = UUID.fromString(weddingId);
            UUID eId = (eventId != null && !eventId.isBlank()) ? UUID.fromString(eventId) : null;
            var list = challengeService.list(wId, eId, includeCounts, activeOnly);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to fetch challenges"));
        }
    }

    // Helplines (multiple)
    @GetMapping("/weddings/{weddingId}/helplines")
    public ResponseEntity<?> listHelplines(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId
    ) {
        try {
            UUID wId = UUID.fromString(weddingId);
            var list = helplineRepository.findByWeddingIdOrderByCreatedAtDesc(wId);
            java.util.List<HelplineDto> out = new java.util.ArrayList<>();
            for (Helpline h : list) {
                out.add(new HelplineDto(h.getId(), h.getTitle(), h.getDescription(), h.getPhone(), h.getEmail(), h.getCreatedAt(), h.getUpdatedAt()));
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to fetch helplines"));
        }
    }

    @PostMapping("/weddings/{weddingId}/helplines")
    public ResponseEntity<?> createHelpline(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @Valid @RequestBody HelplineDto request
    ) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            UUID wId = UUID.fromString(weddingId);
            var m = weddingMemberRepository.findByWeddingIdAndUserId(wId, UUID.fromString(userId));
            if (m.isEmpty() || m.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            Helpline h = new Helpline();
            h.setWeddingId(wId);
            h.setTitle(request.getTitle());
            h.setDescription(request.getDescription());
            h.setPhone(request.getPhone());
            h.setEmail(request.getEmail());
            var saved = helplineRepository.save(h);
            return ResponseEntity.ok(new HelplineDto(saved.getId(), saved.getTitle(), saved.getDescription(), saved.getPhone(), saved.getEmail(), saved.getCreatedAt(), saved.getUpdatedAt()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to create helpline"));
        }
    }

    @PutMapping("/weddings/{weddingId}/helplines/{id}")
    public ResponseEntity<?> updateHelpline(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @PathVariable String id,
            @Valid @RequestBody HelplineDto request
    ) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            UUID wId = UUID.fromString(weddingId);
            var m = weddingMemberRepository.findByWeddingIdAndUserId(wId, UUID.fromString(userId));
            if (m.isEmpty() || m.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            var h = helplineRepository.findById(UUID.fromString(id)).orElseThrow();
            if (!h.getWeddingId().equals(wId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            h.setTitle(request.getTitle());
            h.setDescription(request.getDescription());
            h.setPhone(request.getPhone());
            h.setEmail(request.getEmail());
            var saved = helplineRepository.save(h);
            return ResponseEntity.ok(new HelplineDto(saved.getId(), saved.getTitle(), saved.getDescription(), saved.getPhone(), saved.getEmail(), saved.getCreatedAt(), saved.getUpdatedAt()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to update helpline"));
        }
    }

    @DeleteMapping("/weddings/{weddingId}/helplines/{id}")
    public ResponseEntity<?> deleteHelpline(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @PathVariable String id
    ) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            UUID wId = UUID.fromString(weddingId);
            var m = weddingMemberRepository.findByWeddingIdAndUserId(wId, UUID.fromString(userId));
            if (m.isEmpty() || m.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            var h = helplineRepository.findById(UUID.fromString(id)).orElseThrow();
            if (!h.getWeddingId().equals(wId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            helplineRepository.delete(h);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to delete helpline"));
        }
    }

    @PostMapping("/weddings/{weddingId}/challenges")
    public ResponseEntity<?> createChallenge(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @Valid @RequestBody CreateChallengeDto request
    ) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            UUID wId = UUID.fromString(weddingId);
            // Admin check
            var m = weddingMemberRepository.findByWeddingIdAndUserId(wId, UUID.fromString(userId));
            if (m.isEmpty() || m.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            UUID eId = (request.getEventId() != null && !request.getEventId().isBlank()) ? UUID.fromString(request.getEventId()) : null;
            java.time.Instant start = null;
            java.time.Instant end = null;
            if (request.getStartAtIso() != null && !request.getStartAtIso().isBlank()) start = java.time.Instant.parse(request.getStartAtIso());
            if (request.getEndAtIso() != null && !request.getEndAtIso().isBlank()) end = java.time.Instant.parse(request.getEndAtIso());
            var dto = challengeService.create(wId, request.getTag(), request.getTitle(), request.getDescription(), eId, request.getActive(), start, end);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to create challenge"));
        }
    }

    @PutMapping("/weddings/{weddingId}/challenges/{id}")
    public ResponseEntity<?> updateChallenge(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @PathVariable String id,
            @Valid @RequestBody CreateChallengeDto request
    ) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            UUID wId = UUID.fromString(weddingId);
            var m = weddingMemberRepository.findByWeddingIdAndUserId(wId, UUID.fromString(userId));
            if (m.isEmpty() || m.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            UUID eId = (request.getEventId() != null && !request.getEventId().isBlank()) ? UUID.fromString(request.getEventId()) : null;
            java.time.Instant start = null;
            java.time.Instant end = null;
            if (request.getStartAtIso() != null && !request.getStartAtIso().isBlank()) start = java.time.Instant.parse(request.getStartAtIso());
            if (request.getEndAtIso() != null && !request.getEndAtIso().isBlank()) end = java.time.Instant.parse(request.getEndAtIso());
            var dto = challengeService.update(UUID.fromString(id), request.getTag(), request.getTitle(), request.getDescription(), eId, request.getActive(), start, end);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to update challenge"));
        }
    }

    @DeleteMapping("/weddings/{weddingId}/challenges/{id}")
    public ResponseEntity<?> deleteChallenge(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId,
            @PathVariable String id
    ) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            UUID wId = UUID.fromString(weddingId);
            var m = weddingMemberRepository.findByWeddingIdAndUserId(wId, UUID.fromString(userId));
            if (m.isEmpty() || m.get().getRole() != com.example.demo.models.MemberRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("FORBIDDEN", "Not authorized"));
            }
            challengeService.delete(UUID.fromString(id));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseDto("INTERNAL_ERROR", "Failed to delete challenge"));
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


