package com.example.demo.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelplineDto {
    private UUID id;
    private String title;
    private String description;
    private String phone;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;
}
