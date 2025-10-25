package com.example.demo.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateEventDto {
    private String venueId; // optional
    @NotBlank
    private String title;
    private String description;
    // Single datetime input; backend will derive endTime
    @NotBlank
    private String startTimeIso;
    // Optional; if missing/blank, backend will default to +2h
    private String endTimeIso;
}
