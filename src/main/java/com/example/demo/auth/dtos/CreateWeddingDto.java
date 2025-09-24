package com.example.demo.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWeddingDto {
    
    @NotBlank(message = "Wedding code is required")
    @Size(min = 6, max = 20, message = "Wedding code must be between 6 and 20 characters")
    private String code;
    
    @NotBlank(message = "Wedding title is required")
    @Size(max = 100, message = "Wedding title must be less than 100 characters")
    private String title;
    
    private String partner1;
    private String partner2;
    private String coverImageUrl;
}
