package com.example.demo.controllers;

import com.example.demo.auth.dtos.*;
import com.example.demo.auth.jwt.JwtIssuer;
import com.example.demo.auth.util.EmailUtil;
import com.example.demo.auth.vendor.Msg91OtpClient;
import com.example.demo.models.User;
import com.example.demo.services.UserService;
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

                            // Generate JWT tokens
                            String accessToken = jwtIssuer.accessToken(user.getId().toString(), user.getEmail());
                            String refreshToken = jwtIssuer.refreshToken(user.getId().toString());

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
                                    5400, // 90 minutes in seconds
                                    refreshToken
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
}
