package com.example.demo.auth.dtos;

import com.example.demo.models.MediaType;
import com.example.demo.models.TranscodeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostMediaDto {
    
    private UUID id;
    private MediaType type;
    private String url;
    private String mimeType;
    private long sizeBytes;
    private Integer durationSec;
    private int orderIndex;
    private TranscodeStatus transcodeStatus;
    private Instant createdAt;
}
