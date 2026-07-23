package com.medsupply.platform.modules.fcm.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.fcm.service.FcmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fcm")
@Tag(name = "Firebase Cloud Messaging", description = "Endpoints for pushing notifications to web and mobile clients.")
public class FcmController {

    private final FcmService fcmService;

    public FcmController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    @PostMapping("/send-direct")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send push to single token", description = "Dispatches push alert directly to a registered client token.")
    public ResponseEntity<ApiResponse<Void>> sendDirectNotification(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String title = payload.get("title");
        String body = payload.get("body");

        if (token == null || token.trim().isEmpty() || title == null || body == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("MISSING_PARAMS", "Token, title and body are required fields."));
        }

        fcmService.sendNotification(token, title, body);
        return ResponseEntity.ok(ApiResponse.success(null, "Direct push triggered."));
    }

    @PostMapping("/send-topic")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send push notification to all users subscribed to a specific topic", description = "Dispatches announcement updates to topic listeners.")
    public ResponseEntity<ApiResponse<Void>> sendTopicNotification(@RequestBody Map<String, String> payload) {
        String topic = payload.get("topic");
        String title = payload.get("title");
        String body = payload.get("body");

        if (topic == null || topic.trim().isEmpty() || title == null || body == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("MISSING_PARAMS", "Topic, title and body are required fields."));
        }

        fcmService.sendTopicNotification(topic, title, body);
        return ResponseEntity.ok(ApiResponse.success(null, "Topic broadcast triggered."));
    }
}
