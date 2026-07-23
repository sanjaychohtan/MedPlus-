package com.medsupply.platform.modules.s3.service.impl;

import com.medsupply.platform.common.exception.DomainException;
import com.medsupply.platform.modules.s3.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3ServiceImpl implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket-name:medsupply-enterprise-vault}")
    private String bucketName;

    @Value("${app.aws.s3.access-key:}")
    private String accessKey;

    public S3ServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    private boolean isMockMode() {
        return accessKey == null || accessKey.trim().isEmpty();
    }

    @Override
    public String uploadFile(String prefix, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = prefix + "/" + UUID.randomUUID().toString() + extension;

        if (isMockMode()) {
            log.info("[MOCK S3] Uploading file to {} in bucket {}", key, bucketName);
            // Simulated upload path
            return "https://" + bucketName + ".s3.amazonaws.com/" + key;
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Successfully uploaded file to S3: {}", key);
            return "https://" + bucketName + ".s3.amazonaws.com/" + key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new DomainException("S3_UPLOAD_FAILED", "Could not upload file to cloud storage: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String generatePresignedUrl(String key, int expirationMinutes) {
        if (isMockMode()) {
            log.info("[MOCK S3] Generating pre-signed URL for key: {}", key);
            return "https://" + bucketName + ".s3.amazonaws.com/" + key + "?mock-signature=true&expires=" + expirationMinutes;
        }

        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(b -> b.bucket(bucketName).key(key))
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate pre-signed URL: {}", e.getMessage(), e);
            throw new DomainException("PRESIGN_URL_FAILED", "Could not generate secure cloud URL: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteFile(String key) {
        if (isMockMode()) {
            log.info("[MOCK S3] Deleting file key: {}", key);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted file from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage(), e);
            throw new DomainException("S3_DELETE_FAILED", "Could not delete file from cloud storage: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
