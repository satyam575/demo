package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aws.s3")
public class AwsS3Properties {
    
    private String accessKeyId;
    private String secretAccessKey;
    private String region;
    private String bucketName;
    private String publicUrl;
    
    // Media upload settings
    private int maxFileSizeMB = 50;
    private String[] allowedImageTypes = {"image/jpeg", "image/png", "image/gif", "image/webp"};
    private String[] allowedVideoTypes = {"video/mp4", "video/quicktime", "video/x-msvideo"};
    private String[] allowedAudioTypes = {"audio/mpeg", "audio/wav", "audio/ogg"};
}
