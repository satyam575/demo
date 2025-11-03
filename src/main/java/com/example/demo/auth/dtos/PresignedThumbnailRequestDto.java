package com.example.demo.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedThumbnailRequestDto {
    @NotBlank(message = "originalObjectKey is required")
    private String originalObjectKey;
}

