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
public class MemberProfileDto {
    private UUID memberId;
    private UUID weddingId;
    private UUID userId;
    private String name;
    private String displayName;
    private String avatarUrl;
    private MemberRole role;
    private MemberStatus status;
    private Instant joinedAt;
    private long postCount;
}

