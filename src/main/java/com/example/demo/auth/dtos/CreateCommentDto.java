package com.example.demo.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentDto {
    
    @NotNull(message = "Post ID is required")
    private String postId;
    
    @NotBlank(message = "Content text is required")
    @Size(max = 500, message = "Content text must be less than 500 characters")
    private String contentText;
}
