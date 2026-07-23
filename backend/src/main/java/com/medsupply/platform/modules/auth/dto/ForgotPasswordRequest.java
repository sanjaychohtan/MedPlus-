package com.medsupply.platform.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Capture payload used to initiate password recovery workflows.
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Recovery email address is required")
    @Email(message = "Please provide a valid email format")
    private String email;
}
