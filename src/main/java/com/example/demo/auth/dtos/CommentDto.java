package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    
    private UUID id;
    private UUID postId;
    private UUID authorMemberId;
    private String authorName;
    private String contentText;
    private Instant createdAt;
    private Instant updatedAt;
}
