package com.medsupply.platform.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Capture payload used to apply password overrides via cryptographic recovery tokens.
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Cryptographic recovery token is required")
    private String token;

    @NotBlank(message = "New password credential is required")
    @Size(min = 8, max = 100, message = "New password must lie between 8 and 100 characters")
    private String newPassword;
}
