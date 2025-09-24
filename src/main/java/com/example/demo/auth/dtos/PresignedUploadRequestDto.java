package com.example.demo.auth.dtos;

import com.example.demo.models.MediaType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadRequestDto {
    
    @NotNull(message = "Wedding ID is required")
    private String weddingId;
    
    @NotNull(message = "Media type is required")
    private MediaType mediaType;
    
    @NotNull(message = "Content type is required")
    @Size(max = 100, message = "Content type must be less than 100 characters")
    private String contentType;
    
    private Long fileSize;
}
