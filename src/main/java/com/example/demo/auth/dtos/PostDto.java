package com.example.demo.auth.dtos;

import com.example.demo.models.PostVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    
    private UUID id;
    private UUID weddingId;
    private UUID authorMemberId;
    private String authorName;
    private String authorDisplayName;
    private String contentText;
    private PostVisibility visibility;
    private int mediaCount;
    private List<PostMediaDto> media;
    private int likeCount;
    private int commentCount;
    private boolean isLikedByUser;
    private Instant createdAt;
    private Instant updatedAt;
}
