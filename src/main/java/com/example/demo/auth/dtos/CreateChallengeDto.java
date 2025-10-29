package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChallengeDto {
    private String tag; // required, with or without '#'
    private String title; // required
    private String description; // optional
    private String eventId; // optional
    private Boolean active; // optional
    private String startAtIso; // optional ISO
    private String endAtIso; // optional ISO
}

