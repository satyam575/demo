package com.example.demo.auth.dtos;

import com.example.demo.models.PostVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostDto {
    
    @NotNull(message = "Wedding ID is required")
    private String weddingId;
    
    @Size(max = 2000, message = "Content text must be less than 2000 characters")
    private String contentText;
    
    @NotNull(message = "Visibility is required")
    private PostVisibility visibility;
    
    private List<String> mediaUrls; // URLs of uploaded media files

    // Optional association to a function/event
    private String eventId;
}
