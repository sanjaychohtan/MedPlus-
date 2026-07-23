package com.medsupply.platform.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Capture payload containing verification inputs for OTP registration handshakes.
 */
@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required for verification")
    @Email(message = "Please provide a valid email format")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP must consist of exactly 6 digits")
    private String otp;
}
