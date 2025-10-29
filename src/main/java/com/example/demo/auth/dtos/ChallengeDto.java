package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {
    private UUID id;
    private UUID weddingId;
    private UUID eventId; // nullable
    private String tag;
    private String title;
    private String description;
    private boolean active;
    private Instant startAt;
    private Instant endAt;
    private Counts counts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counts {
        private long posts;
    }
}

