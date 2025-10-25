package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private UUID id;
    private UUID weddingId;
    private UUID venueId;
    private String title;
    private String description;
    private Instant startTime;
}
