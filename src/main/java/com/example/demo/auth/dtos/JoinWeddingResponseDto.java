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
public class JoinWeddingResponseDto {
    
    private UUID id;
    private UUID weddingId;
    private String weddingTitle;
    private String weddingCode;
    private MemberRole role;
    private MemberStatus status;
    private String displayName;
    private Instant joinedAt;
    private boolean isNewMember;
}
