package com.example.demo.auth.dtos;

import com.example.demo.models.MemberRole;
import com.example.demo.models.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWeddingDto {
    
    private UUID id;
    private UUID weddingId;
    private UUID userId;
    private String weddingTitle;
    private String weddingCode;
    private String partner1;
    private String partner2;
    private String coverImageUrl;
    private MemberRole role;
    private MemberStatus status;
    private String displayName;
    private Instant joinedAt;
    private boolean isActive;
}