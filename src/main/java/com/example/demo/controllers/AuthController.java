package com.example.demo.controllers;

import com.example.demo.auth.dtos.*;
import com.example.demo.auth.jwt.JwtIssuer;
import com.example.demo.auth.util.EmailUtil;
import com.example.demo.auth.vendor.Msg91OtpClient;
import com.example.demo.models.User;
import com.example.demo.services.UserService;
import com.example.demo.services.WeddingService;
import com.example.demo.services.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final Msg91OtpClient msg91OtpClient;
    private final UserService userService;
    private final JwtIssuer jwtIssuer;
    private final WeddingService weddingService;
    private final S3Service s3Service;

    @PostMapping("/otp/request")
    public Mono<ResponseEntity<?>> requestOtp(@Valid @RequestBody OtpRequestDto request) {
        try {
            String normalizedEmail = EmailUtil.normalizeEmail(request.getEmail());

            return msg91OtpClient.generateOtp(normalizedEmail)
                    .map(response -> {
                        if ("success".equals(response.getType())) {
                            OtpRequestResponseDto responseDto = new OtpRequestResponseDto(
                                    response.getRequestId(),
                                    300, // 5 minutes
                                    30   // 30 seconds
                            );

                            return ResponseEntity.ok(responseDto);
                        } else {
                            ErrorResponseDto error = new ErrorResponseDto(
                                    "OTP_SEND_FAILED",
                                    response.getMessage()
                            );
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                        }
                    })
                    .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ErrorResponseDto("OTP_SEND_ERROR", "Failed to send OTP")));

        } catch (IllegalArgumentException e) {
            log.error("Invalid email: {}", request.getEmail(), e);
            ErrorResponseDto error = new ErrorResponseDto("VALIDATION_ERROR", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        } catch (Exception e) {
            log.error("Unexpected error in OTP request", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
        }
    }

    @PostMapping("/otp/verify")
    public Mono<ResponseEntity<?>> verifyOtp(@Valid @RequestBody OtpVerifyDto request) {
        try {
            String normalizedEmail = EmailUtil.normalizeEmail(request.getEmail());

            return msg91OtpClient.verifyOtp(request.getRequestId(), request.getCode())
                    .map(response -> {
                        if (response.isSuccess()) {
                            // OTP verified successfully - find or create user by email
                            User user = userService.upsertUserByEmail(normalizedEmail);
                            boolean isNewUser = userService.isNewUserByEmail(normalizedEmail);

                            // Generate JWT token
                            String accessToken = jwtIssuer.accessToken(user.getId().toString(), user.getEmail());

                            // Prepare response
                            UserDto userDto = new UserDto(
                                    user.getId(),
                                    user.getName(),
                                    user.getUsername(),
                                    user.getEmail(),
                                    user.getPhone(),
                                    user.getAvatarUrl(),
                                    user.isPhoneVerified(),
                                    user.isEmailVerified(),
                                    user.getCreatedAt(),
                                    user.getUpdatedAt(),
                                    isNewUser
                            );

                            TokenDto tokenDto = new TokenDto(
                                    accessToken,
                                    604800 // 7 days in seconds (7 * 24 * 60 * 60 = 604800)
                            );

                            OtpVerifyResponseDto responseDto = new OtpVerifyResponseDto(
                                    true,
                                    userDto,
                                    tokenDto
                            );

                            log.info("OTP verification successful for user: {}", user.getId());
                            return ResponseEntity.ok(responseDto);

                        } else {
                            ErrorResponseDto error = new ErrorResponseDto("INVALID_CODE", response.getMessage());
                            return ResponseEntity.badRequest().body(error);
                        }
                    })
                    .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ErrorResponseDto("OTP_VERIFY_ERROR", "Failed to verify OTP")));

        } catch (IllegalArgumentException e) {
            log.error("Invalid email: {}", request.getEmail(), e);
            ErrorResponseDto error = new ErrorResponseDto("VALIDATION_ERROR", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        } catch (Exception e) {
            log.error("Unexpected error in OTP verification", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
        }
    }

    @PostMapping("/profile/update")
    public ResponseEntity<?> updateUserProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UserProfileUpdateDto request) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            // Update user profile
            User updatedUser = userService.updateUser(
                    java.util.UUID.fromString(userId),
                    request.getName(),
                    request.getEmail(),
                    request.getUsername(),
                    request.getAvatarUrl()
            );

            // Prepare response
            UserDto userDto = new UserDto(
                    updatedUser.getId(),
                    updatedUser.getName(),
                    updatedUser.getUsername(),
                    updatedUser.getEmail(),
                    updatedUser.getPhone(),
                    updatedUser.getAvatarUrl(),
                    updatedUser.isPhoneVerified(),
                    updatedUser.isEmailVerified(),
                    updatedUser.getCreatedAt(),
                    updatedUser.getUpdatedAt(),
                    false // Not a new user
            );

            log.info("User profile updated for user: {}", updatedUser.getId());
            return ResponseEntity.ok(userDto);

        } catch (Exception e) {
            log.error("Unexpected error in profile update", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/wedding/join")
    public ResponseEntity<?> joinWedding(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody JoinWeddingDto request) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            // Join wedding
            JoinWeddingResponseDto response = weddingService.joinWedding(
                    java.util.UUID.fromString(userId),
                    request.getCode(),
                    request.getDisplayName()
            );

            log.info("User {} joined wedding {}", userId, request.getCode());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid wedding code: {}", request.getCode(), e);
            ErrorResponseDto error = new ErrorResponseDto("WEDDING_NOT_FOUND", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in wedding join", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/weddings")
    public ResponseEntity<?> getUserWeddings(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            // Get user weddings
            java.util.List<UserWeddingDto> weddings = weddingService.getUserWeddings(java.util.UUID.fromString(userId));

            log.info("Retrieved {} weddings for user {}", weddings.size(), userId);
            return ResponseEntity.ok(weddings);

        } catch (Exception e) {
            log.error("Unexpected error in getting user weddings", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/wedding/create")
    public ResponseEntity<?> createWedding(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CreateWeddingDto request) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            // Create wedding and add creator as ADMIN member
            com.example.demo.models.Wedding wedding = weddingService.createWedding(
                    java.util.UUID.fromString(userId),
                    request.getCode(),
                    request.getTitle(),
                    request.getPartner1(),
                    request.getPartner2(),
                    request.getCoverImageUrl()
            );

            log.info("User {} created wedding {}", userId, request.getCode());
            return ResponseEntity.ok(wedding);

        } catch (IllegalArgumentException e) {
            log.error("Invalid wedding creation request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("WEDDING_CREATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in wedding creation", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/weddings/{weddingId}")
    public ResponseEntity<?> deleteWedding(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String weddingId) {
        try {
            // Extract user ID from JWT token
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);

            // Delete wedding and its bucket
            weddingService.deleteWedding(
                    java.util.UUID.fromString(weddingId),
                    java.util.UUID.fromString(userId)
            );

            log.info("User {} deleted wedding {}", userId, weddingId);
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid wedding deletion request: {}", e.getMessage());
            ErrorResponseDto error = new ErrorResponseDto("WEDDING_DELETION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error in wedding deletion", e);
            ErrorResponseDto error = new ErrorResponseDto("INTERNAL_ERROR", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    

    
    @GetMapping("/media/{weddingId}/{objectKey}")
    public ResponseEntity<?> getMedia(@PathVariable String weddingId, 
                                    @PathVariable String objectKey,
                                    @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String userId = jwtIssuer.getUserIdFromToken(token);
            
            // Verify user has access to this wedding
            // TODO: Add wedding access verification
            
            // Get public media URL
            String fullObjectKey = weddingId + "/" + objectKey;
            String publicUrl = s3Service.getPublicMediaUrl(fullObjectKey);
            
            // Redirect to public URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", publicUrl)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error serving media: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "Media not found"));
        }
    }
    
}
