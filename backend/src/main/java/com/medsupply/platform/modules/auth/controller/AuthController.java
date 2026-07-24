package com.medsupply.platform.modules.auth.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.auth.dto.*;
import com.medsupply.platform.modules.auth.service.AuthService;
import com.medsupply.platform.modules.auth.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing REST routes for user registrations, logins, credentials refreshes, and OTP codes validation.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication Management", description = "Endpoints handling user logins, registrations, token refreshes, and recovery pipelines.")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.security.enable-switch-role:false}")
    private boolean enableSwitchRole;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Validates credentials and registers new accounts. Triggers a 5-minute verification OTP code.")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpServletRequest) {
        String clientIp = resolveClientIp(httpServletRequest);
        authService.register(request, clientIp);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Registration successful. Please verify the OTP code sent to your email."));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies password credentials, resets failure counters, and returns signed access and refresh tokens.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest,
            jakarta.servlet.http.HttpServletResponse httpServletResponse) {
        String clientIp = resolveClientIp(httpServletRequest);
        LoginResponse response = authService.login(request, clientIp);

        // Access Token: HttpOnly, Secure, SameSite=Strict, 15 minutes (900 seconds)
        org.springframework.http.ResponseCookie accessTokenCookie = org.springframework.http.ResponseCookie.from("access_token", response.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();

        // Refresh Token: HttpOnly, Secure, SameSite=Strict, 7 days (604800 seconds)
        org.springframework.http.ResponseCookie refreshTokenCookie = org.springframework.http.ResponseCookie.from("refresh_token", response.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(604800)
                .build();

        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify registration OTP", description = "Validates the registration OTP code and activates user status.")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpServletRequest) {
        String clientIp = resolveClientIp(httpServletRequest);
        authService.verifyOtp(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(null, "Account successfully verified. You can now log in."));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Regenerates and logs a fresh 5-minute verification code for pending accounts.")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @RequestParam @NotBlank @Email String email,
            HttpServletRequest httpServletRequest) {
        String clientIp = resolveClientIp(httpServletRequest);
        authService.resendOtp(email, clientIp);
        return ResponseEntity.ok(ApiResponse.success(null, "A fresh verification code has been generated and sent."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Generates a 15-minute password reset recovery token for existing accounts.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpServletRequest) {
        String clientIp = resolveClientIp(httpServletRequest);
        authService.forgotPassword(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(null, "If the email is registered, a password recovery link has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token", description = "Overrides account credentials using a valid cryptographic reset token.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpServletRequest) {
        String clientIp = resolveClientIp(httpServletRequest);
        authService.resetPassword(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(null, "Password has been successfully updated. You can now log in."));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Rotates access tokens using a valid refresh token.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshTokenFromCookie,
            @RequestParam(required = false) String refreshToken,
            HttpServletRequest httpServletRequest,
            jakarta.servlet.http.HttpServletResponse httpServletResponse) {
        
        String tokenToUse = refreshTokenFromCookie;
        if (!org.springframework.util.StringUtils.hasText(tokenToUse)) {
            tokenToUse = refreshToken;
        }

        if (!org.springframework.util.StringUtils.hasText(tokenToUse)) {
            throw new com.medsupply.platform.common.exception.DomainException(
                "MISSING_REFRESH_TOKEN", "Refresh token cookie or parameter is required.", HttpStatus.UNAUTHORIZED);
        }

        String clientIp = resolveClientIp(httpServletRequest);
        LoginResponse response = authService.refreshAccessToken(tokenToUse, clientIp);

        // Access Token: HttpOnly, Secure, SameSite=Strict, 15 minutes (900 seconds)
        org.springframework.http.ResponseCookie accessTokenCookie = org.springframework.http.ResponseCookie.from("access_token", response.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();

        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clears access and refresh token cookies.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @jakarta.servlet.http.CookieValue(value = "refresh_token", required = false) String refreshTokenFromCookie,
            @RequestParam(value = "refreshToken", required = false) String refreshTokenParam,
            HttpServletRequest httpServletRequest,
            jakarta.servlet.http.HttpServletResponse httpServletResponse) {
        
        String tokenToUse = refreshTokenFromCookie;
        if (!org.springframework.util.StringUtils.hasText(tokenToUse)) {
            tokenToUse = refreshTokenParam;
        }

        if (org.springframework.util.StringUtils.hasText(tokenToUse)) {
            String clientIp = resolveClientIp(httpServletRequest);
            try {
                authService.logout(tokenToUse, clientIp);
            } catch (Exception e) {
                // Log and continue to clear cookies
            }
        }

        org.springframework.http.ResponseCookie clearAccess = org.springframework.http.ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        org.springframework.http.ResponseCookie clearRefresh = org.springframework.http.ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, clearAccess.toString());
        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, clearRefresh.toString());

        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves profile details of the currently authenticated user session.")
    public ResponseEntity<ApiResponse<com.medsupply.platform.modules.auth.model.User>> getMe() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new com.medsupply.platform.common.exception.DomainException(
                "UNAUTHORIZED", "User session is not authenticated.", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        com.medsupply.platform.modules.auth.model.User user = authService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user, "User profile loaded successfully"));
    }

    @PostMapping("/switch-role")
    @Operation(summary = "Switch current user role", description = "Development utility to dynamically swap active roles and return a new token.")
    public ResponseEntity<ApiResponse<LoginResponse>> switchRole(
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest httpServletRequest,
            jakarta.servlet.http.HttpServletResponse httpServletResponse) {
        if (!enableSwitchRole) {
            throw new com.medsupply.platform.common.exception.DomainException(
                "ENDPOINT_DISABLED", "Role switching is disabled in this environment.", HttpStatus.FORBIDDEN);
        }

        String roleStr = body.get("role");
        if (roleStr == null) {
            throw new com.medsupply.platform.common.exception.DomainException(
                "MISSING_ROLE", "Role is required.", HttpStatus.BAD_REQUEST);
        }

        // Get current authenticated user's email
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.medsupply.platform.common.exception.DomainException(
                "UNAUTHORIZED", "User must be authenticated to switch roles.", HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();

        com.medsupply.platform.modules.auth.model.User user = authService.getUserByEmail(email);

        // Generate secure tokens with the new role
        java.util.List<String> roles = java.util.Collections.singletonList("ROLE_" + roleStr);
        String accessToken = tokenProvider.generateAccessToken(email, roles);
        String refreshToken = tokenProvider.generateRefreshToken(email);

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build();

        // Access Token: HttpOnly, Secure, SameSite=Strict, 15 minutes (900 seconds)
        org.springframework.http.ResponseCookie accessTokenCookie = org.springframework.http.ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(900)
                .build();

        // Refresh Token: HttpOnly, Secure, SameSite=Strict, 7 days (604800 seconds)
        org.springframework.http.ResponseCookie refreshTokenCookie = org.springframework.http.ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(604800)
                .build();

        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        httpServletResponse.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return ResponseEntity.ok(ApiResponse.success(response, "Role switched successfully"));
    }

    /**
     * Extracts client IP address by prioritizing the 'X-Forwarded-For' proxy header,
     * maintaining high reliability in containerized/load-balanced cloud architectures.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ipList = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ipList)) {
            // Take the first IP if multiple hops exist
            return ipList.split(",")[0].trim();
        }
        String ip = request.getHeader("Proxy-Client-IP");
        if (!StringUtils.hasText(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
