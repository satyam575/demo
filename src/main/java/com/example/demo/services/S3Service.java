package com.example.demo.services;

import com.example.demo.auth.dtos.PresignedUploadResponse;
import com.example.demo.config.AwsS3Properties;
import com.example.demo.models.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {
    
    private final AwsS3Properties awsS3Properties;
    
    private S3Client getS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                awsS3Properties.getAccessKeyId(),
                awsS3Properties.getSecretAccessKey()
        );
        
        return S3Client.builder()
                .region(Region.of(awsS3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
    
    private S3Presigner getS3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                awsS3Properties.getAccessKeyId(),
                awsS3Properties.getSecretAccessKey()
        );
        
        return S3Presigner.builder()
                .region(Region.of(awsS3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
    
    public PresignedUploadResponse generatePresignedUploadUrl(MediaType mediaType, UUID weddingId, String contentType, long fileSize) {
        String bucketName = awsS3Properties.getBucketName();
        String effectiveContentType = (contentType != null && !contentType.isBlank())
                ? contentType
                : switch (mediaType) {
                    case IMAGE -> "image/jpeg";
                    case VIDEO -> "video/mp4";
                    case AUDIO -> "audio/mpeg";
                };
        String fileExtension = getFileExtensionFromContentType(effectiveContentType);
        if (fileExtension == null || fileExtension.isEmpty()) {
            fileExtension = switch (mediaType) {
                case IMAGE -> ".jpg";
                case VIDEO -> ".mp4";
                case AUDIO -> ".mp3";
            };
        }
        String objectKey = generateObjectKey(weddingId, mediaType, fileExtension);
        
        try (S3Presigner presigner = getS3Presigner()) {
            // Set Content-Type and Cache-Control on the object so browsers/CDNs cache aggressively.
            String cacheControl = "public, max-age=31536000, immutable"; // 1 year, versioned keys are immutable
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(effectiveContentType)
                    .cacheControl(cacheControl)
                    .build();
            
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(java.time.Duration.ofMinutes(15))
                    .putObjectRequest(putObjectRequest)
                    .build();
            
            PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            log.info("Generated presigned upload URL for: {}", objectKey);
            
            String mediaUrl = getMediaUrl(objectKey);
            log.info("Generated presigned upload URL - Object Key: {}, Media URL: {}", objectKey, mediaUrl);
            
            PresignedUploadResponse dto = new PresignedUploadResponse();
            dto.setUploadUrl(presignedUrl);
            dto.setObjectKey(objectKey);
            dto.setMediaUrl(mediaUrl);
            dto.setExpiration(presignedRequest.expiration());
            dto.setContentType(effectiveContentType);
            dto.setCacheControl(cacheControl);
            return dto;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for bucket: {}, key: {}", bucketName, objectKey, e);
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }
    
    public String uploadMedia(MultipartFile file, MediaType mediaType, UUID weddingId) throws IOException {
        validateFile(file, mediaType);
        
        String fileExtension = getFileExtension(file.getOriginalFilename());
        if (fileExtension == null || fileExtension.isEmpty()) {
            String fromCt = getFileExtensionFromContentType(file.getContentType());
            if (fromCt != null && !fromCt.isEmpty()) {
                fileExtension = fromCt;
            } else {
                fileExtension = switch (mediaType) {
                    case IMAGE -> ".jpg";
                    case VIDEO -> ".mp4";
                    case AUDIO -> ".mp3";
                };
            }
        }
        String objectKey = generateObjectKey(weddingId, mediaType, fileExtension);
        
        try (S3Client s3Client = getS3Client()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(awsS3Properties.getBucketName())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("Successfully uploaded media to S3: {}", objectKey);
            return objectKey;
        }
    }
    
    public void deleteMedia(String objectKey) {
        try (S3Client s3Client = getS3Client()) {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(awsS3Properties.getBucketName())
                    .key(objectKey)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted media from S3: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to delete media from S3: {}", objectKey, e);
        }
    }
    
    public void deleteWeddingMedia(UUID weddingId) {
        try (S3Client s3Client = getS3Client()) {
            String bucketName = awsS3Properties.getBucketName();
            
            var listResponse = s3Client.listObjectsV2(b -> b
                .bucket(bucketName)
                .prefix(weddingId.toString() + "/")
            );
            
            if (listResponse.contents() != null) {
                listResponse.contents().forEach(object -> {
                    s3Client.deleteObject(b -> b.bucket(bucketName).key(object.key()));
                });
                log.info("Successfully deleted {} media files for wedding: {}", 
                    listResponse.contents().size(), weddingId);
            }
        } catch (Exception e) {
            log.error("Failed to delete wedding media for wedding: {}", weddingId, e);
        }
    }
    
    public String getMediaUrl(String objectKey) {
        return awsS3Properties.getPublicUrl() + "/" + objectKey;
    }
    
    public String getPublicMediaUrl(String objectKey) {
        return awsS3Properties.getPublicUrl() + "/" + objectKey;
    }
    
    private void validateFile(MultipartFile file, MediaType mediaType) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        validateFileSize(file.getSize());
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File content type is null");
        }
        
        validateFileType(contentType, mediaType);
    }
    
    private void validateFileSize(long fileSize) {
        if (fileSize > awsS3Properties.getMaxFileSizeMB() * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + awsS3Properties.getMaxFileSizeMB() + "MB");
        }
    }
    
    private void validateFileType(String contentType, MediaType mediaType) {
        switch (mediaType) {
            case IMAGE:
                if (!isAllowedImageType(contentType)) {
                    throw new IllegalArgumentException("Invalid image type: " + contentType);
                }
                break;
            case VIDEO:
                if (!isAllowedVideoType(contentType)) {
                    throw new IllegalArgumentException("Invalid video type: " + contentType);
                }
                break;
            case AUDIO:
                if (!isAllowedAudioType(contentType)) {
                    throw new IllegalArgumentException("Invalid audio type: " + contentType);
                }
                break;
        }
    }
    
    private boolean isAllowedImageType(String contentType) {
        for (String allowedType : awsS3Properties.getAllowedImageTypes()) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isAllowedVideoType(String contentType) {
        for (String allowedType : awsS3Properties.getAllowedVideoTypes()) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isAllowedAudioType(String contentType) {
        for (String allowedType : awsS3Properties.getAllowedAudioTypes()) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    private String getFileExtensionFromContentType(String contentType) {
        if (contentType == null) return "";

        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            case "image/heic":
                return ".heic";
            case "image/heif":
                return ".heif";
            case "video/mp4":
                return ".mp4";
            case "video/quicktime":
                return ".mov";
            case "video/x-msvideo":
                return ".avi";
            case "audio/mpeg":
                return ".mp3";
            case "audio/wav":
                return ".wav";
            case "audio/ogg":
                return ".ogg";
            default:
                return "";
        }
    }
    
    private String generateObjectKey(UUID weddingId, MediaType mediaType, String fileExtension) {
        String objectId = UUID.randomUUID().toString();
        return String.format("%s/%s%s", 
                weddingId.toString(), 
                objectId, 
                fileExtension);
    }
}
