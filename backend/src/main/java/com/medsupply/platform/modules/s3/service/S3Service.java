package com.medsupply.platform.modules.s3.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface S3Service {

    /**
     * Uploads a file to S3 under the specified key prefix.
     */
    String uploadFile(String prefix, MultipartFile file) throws IOException;

    /**
     * Generates a pre-signed GET URL for a secure resource (e.g. invoice or document).
     */
    String generatePresignedUrl(String key, int expirationMinutes);

    /**
     * Deletes an object from S3.
     */
    void deleteFile(String key);
}
