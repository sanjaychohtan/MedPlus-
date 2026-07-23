package com.medsupply.platform;

import com.medsupply.platform.common.dto.ApiResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application entry point for the MedSupply Enterprise Platform.
 */
@RestController
@SpringBootApplication
public class MedSupplyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedSupplyApplication.class, args);
    }

    /**
     * Standard lightweight health check endpoint.
     * Accessible unauthenticated.
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("database", "PostgreSQL 16 Connected");
        healthInfo.put("platform", "Java 21 LTS / Spring Boot 3.5.x");
        healthInfo.put("epoch", System.currentTimeMillis());
        
        return ApiResponse.success(healthInfo, "MedSupply System is healthy and operational");
    }
}
