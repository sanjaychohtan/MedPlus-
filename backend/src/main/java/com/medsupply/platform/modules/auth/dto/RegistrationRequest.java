package com.medsupply.platform.modules.auth.dto;

import com.medsupply.platform.modules.auth.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Capture payload containing validation constraints for registering new system accounts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must lie between 8 and 100 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @NotNull(message = "Requested role registration selection is required")
    private UserRole role;

    // Optional fields required only for institutional B2B Customers
    @Size(max = 100, message = "Drug license details cannot exceed 100 characters")
    private String licenseNumber;

    @Size(max = 20, message = "GSTIN details cannot exceed 20 characters")
    private String gstin;
}
