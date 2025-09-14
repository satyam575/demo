package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private boolean phoneVerified;
    private boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isNewUser;
}
