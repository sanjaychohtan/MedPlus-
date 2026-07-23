package com.medsupply.platform.modules.s3.service.impl;

import com.medsupply.platform.modules.s3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3ServiceImpl s3Service;

    @BeforeEach
    void setUp() {
        s3Service = new S3ServiceImpl(s3Client, s3Presigner);
    }

    @Test
    void testUploadFileMockMode() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "aspirin.jpg", "image/jpeg", "dummy-image-content".getBytes());

        String url = s3Service.uploadFile("product-images", file);

        assertNotNull(url);
        assertTrue(url.contains("medsupply-enterprise-vault"));
        assertTrue(url.contains("product-images"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void testGeneratePresignedUrlMockMode() {
        String key = "documents/tax-info.pdf";
        String presignedUrl = s3Service.generatePresignedUrl(key, 15);

        assertNotNull(presignedUrl);
        assertTrue(presignedUrl.contains(key));
        assertTrue(presignedUrl.contains("mock-signature=true"));
    }

    @Test
    void testDeleteFileMockMode() {
        // Assert no exception is thrown in mock mode
        assertDoesNotThrow(() -> s3Service.deleteFile("documents/test.pdf"));
    }
}
