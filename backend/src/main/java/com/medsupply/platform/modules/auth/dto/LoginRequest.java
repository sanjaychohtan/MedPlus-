package com.medsupply.platform.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Capture payload containing validation constraints for authenticating existing system accounts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Login email address is required")
    @Email(message = "Please provide a valid email format")
    private String email;

    @NotBlank(message = "Login password credential is required")
    private String password;
}
