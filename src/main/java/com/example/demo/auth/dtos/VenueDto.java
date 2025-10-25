package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueDto {
    private UUID id;
    private UUID weddingId;
    private String name;
    private String address;
    private String mapsUrl;
    private String notes;
    private Instant createdAt;
}

