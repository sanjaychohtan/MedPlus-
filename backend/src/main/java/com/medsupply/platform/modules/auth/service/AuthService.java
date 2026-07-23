package com.medsupply.platform.modules.auth.service;

import com.medsupply.platform.modules.auth.dto.*;

/**
 * Service contract for managing user authentication and account lifecycle flows.
 * Connects directly to the security audit logs ledger.
 */
public interface AuthService {

    /**
     * Validates credentials and generates access/refresh JWT tokens.
     */
    LoginResponse login(LoginRequest request, String clientIp);

    /**
     * Registers a new account and handles role configuration, password hashing, and OTP generation.
     */
    void register(RegistrationRequest request, String clientIp);

    /**
     * Validates account OTP codes and transitions status from PENDING_APPROVAL to ACTIVE.
     */
    void verifyOtp(VerifyOtpRequest request, String clientIp);

    /**
     * Resends a fresh, 5-minute expired OTP code to unverified user accounts.
     */
    void resendOtp(String email, String clientIp);

    /**
     * Generates a 15-minute cryptographically secure recovery token to support password resets.
     */
    void forgotPassword(ForgotPasswordRequest request, String clientIp);

    /**
     * Validates reset tokens and applies password updates securely.
     */
    void resetPassword(ResetPasswordRequest request, String clientIp);

    /**
     * Rotates access tokens using a secure refresh token.
     */
    LoginResponse refreshAccessToken(String refreshToken, String clientIp);

    /**
     * Development utility to retrieve a user profile by email for role switching.
     */
    com.medsupply.platform.modules.auth.model.User getUserByEmail(String email);
}
