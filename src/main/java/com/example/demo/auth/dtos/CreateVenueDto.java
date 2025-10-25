package com.example.demo.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateVenueDto {
    @NotBlank
    private String name;
    private String address;
    private String mapsUrl;
    private String notes;
}

