package com.medsupply.platform.modules.s3.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/s3")
@Tag(name = "AWS S3 File Storage", description = "Endpoints for secure cloud document and image handling.")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload file to secure S3 storage", description = "Uploads file with prefix categorizations: 'product-images', 'documents', 'invoices', or 'profile-images'.")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("prefix") String prefix,
            @RequestParam("file") MultipartFile file) throws IOException {

        String fileUrl = s3Service.uploadFile(prefix, file);
        Map<String, String> response = new HashMap<>();
        response.put("fileUrl", fileUrl);

        // Derive S3 key from the generated URL
        String bucketDomain = ".amazonaws.com/";
        String key = "";
        if (fileUrl.contains(bucketDomain)) {
            key = fileUrl.substring(fileUrl.indexOf(bucketDomain) + bucketDomain.length());
        } else {
            // Mock fallback formatting
            key = prefix + "/" + file.getOriginalFilename();
        }
        response.put("key", key);

        return ResponseEntity.ok(ApiResponse.success(response, "File uploaded successfully"));
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate secure cloud GET URL", description = "Generates temporary pre-signed S3 URL valid for safe browser retrievals.")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam("key") String key,
            @RequestParam(value = "expiryMinutes", defaultValue = "15") int expiryMinutes) {

        String url = s3Service.generatePresignedUrl(key, expiryMinutes);
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        return ResponseEntity.ok(ApiResponse.success(response, "Pre-signed URL generated successfully"));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete object from S3 storage", description = "Performs irreversible delete operations on the cloud storage container.")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam("key") String key) {
        s3Service.deleteFile(key);
        return ResponseEntity.ok(ApiResponse.success(null, "File deleted successfully"));
    }
}
