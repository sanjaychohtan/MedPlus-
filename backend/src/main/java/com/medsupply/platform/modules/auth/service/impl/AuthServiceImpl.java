package com.medsupply.platform.modules.auth.service.impl;

import com.medsupply.platform.common.exception.DomainException;
import com.medsupply.platform.modules.audit.service.AuditLogService;
import com.medsupply.platform.modules.auth.dto.*;
import com.medsupply.platform.modules.auth.model.*;
import com.medsupply.platform.modules.auth.repository.RoleRepository;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.auth.security.JwtTokenProvider;
import com.medsupply.platform.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation managing user registration, logins, OTP handshakes, and token rotations.
 * Directs mutations into the security audit ledger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedAttempts;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        log.debug("Processing login request for user: {}", request.getEmail());
        
        try {
            // 1. Perform Authentication via standard Spring provider
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 2. Resolve user record upon successful credential match
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new DomainException("USER_NOT_FOUND", "User profile not found", HttpStatus.NOT_FOUND));

            // Check if user is active/approved
            if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_BLOCKED_PENDING", "AUTH", 
                        "Login blocked: Account is pending OTP approval", clientIp);
                throw new DomainException("PENDING_APPROVAL", "Your account is pending verification. Please verify your OTP code.", HttpStatus.FORBIDDEN);
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_BLOCKED_SUSPENDED", "AUTH", 
                        "Login blocked: Account is suspended due to too many failed attempts", clientIp);
                throw new DomainException("ACCOUNT_SUSPENDED", "Your account has been suspended. Please contact customer service.", HttpStatus.FORBIDDEN);
            }
            if (user.getStatus() == UserStatus.DEACTIVATED) {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_BLOCKED_DEACTIVATED", "AUTH", 
                        "Login blocked: Account is deactivated", clientIp);
                throw new DomainException("ACCOUNT_DEACTIVATED", "Your account has been deactivated.", HttpStatus.FORBIDDEN);
            }

            // Reset failed login tracking
            user.resetFailedLogins();
            userRepository.save(user);

            // 3. Generate secure tokens
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // Write success trace to global security audit log
            auditLogService.log(user.getId(), roles.get(0), "LOGIN_SUCCESS", "AUTH", "User successfully authenticated", clientIp);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .roles(roles)
                    .build();

        } catch (BadCredentialsException ex) {
            // Handle and throttle failed login attempts
            handleFailedLogin(request.getEmail(), clientIp);
            throw ex;
        }
    }

    private void handleFailedLogin(String email, String clientIp) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.incrementFailedLogins();
            
            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setStatus(UserStatus.SUSPENDED);
                log.warn("User account {} has been SUSPENDED due to {} consecutive failed login attempts.", email, maxFailedAttempts);
                auditLogService.log(user.getId(), "ANONYMOUS", "ACCOUNT_AUTO_LOCKOUT", "AUTH", 
                        "Account suspended automatically due to " + maxFailedAttempts + " failed attempts", clientIp);
            } else {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_ATTEMPT_FAILED", "AUTH", 
                        "Failed login attempt #" + user.getFailedLoginAttempts(), clientIp);
            }
            userRepository.save(user);
        } else {
            auditLogService.log(null, "ANONYMOUS", "LOGIN_ATTEMPT_INVALID_USER", "AUTH", 
                    "Attempted login with non-existent email: " + email, clientIp);
        }
    }

    @Override
    @Transactional
    public void register(RegistrationRequest request, String clientIp) {
        log.debug("Processing registration request for: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DomainException("EMAIL_ALREADY_EXISTS", "This email address is already in use.", HttpStatus.CONFLICT);
        }

        // Map and resolve requested role
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new DomainException("ROLE_NOT_FOUND", "The requested registration role does not exist.", HttpStatus.NOT_FOUND));

        // Generate 6-digit random numeric verification OTP code
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        OffsetDateTime otpExpiry = OffsetDateTime.now().plusMinutes(5);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .licenseNumber(request.getLicenseNumber())
                .gstin(request.getGstin())
                .status(UserStatus.PENDING_APPROVAL)
                .otpCode(otpCode)
                .otpExpiry(otpExpiry)
                .roles(new HashSet<>(Collections.singletonList(role)))
                .build();

        userRepository.save(user);
        
        // Log secure verification output containing generated OTP mock trace for developers in preview environment
        log.info("REGISTRATION COMPLETED. Verification OTP for {} is: {}", request.getEmail(), otpCode);

        auditLogService.log(user.getId(), "ANONYMOUS", "USER_REGISTRATION", "AUTH", 
                "Registered new user account in state PENDING_APPROVAL with role: " + role.getName(), clientIp);
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request, String clientIp) {
        log.debug("Verifying OTP for user: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", "User profile not found", HttpStatus.NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new DomainException("USER_ALREADY_ACTIVE", "This account has already been verified and is active.", HttpStatus.BAD_REQUEST);
        }

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            throw new DomainException("INVALID_OTP", "The verification code is incorrect.", HttpStatus.BAD_REQUEST);
        }

        if (user.getOtpExpiry().isBefore(OffsetDateTime.now())) {
            throw new DomainException("EXPIRED_OTP", "The verification code has expired. Please request a new code.", HttpStatus.BAD_REQUEST);
        }

        // Activate account
        user.setStatus(UserStatus.ACTIVE);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
        auditLogService.log(user.getId(), mainRole, "OTP_VERIFICATION_SUCCESS", "AUTH", "Account successfully activated", clientIp);
    }

    @Override
    @Transactional
    public void resendOtp(String email, String clientIp) {
        log.debug("Resending OTP to email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", "User profile not found", HttpStatus.NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new DomainException("USER_ALREADY_ACTIVE", "This account is already active.", HttpStatus.BAD_REQUEST);
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        user.setOtpCode(otpCode);
        user.setOtpExpiry(OffsetDateTime.now().plusMinutes(5));
        userRepository.save(user);

        log.info("RESENT OTP COMPLETED. Fresh verification OTP for {} is: {}", email, otpCode);

        auditLogService.log(user.getId(), "ANONYMOUS", "OTP_RESEND", "AUTH", "OTP code regenerated", clientIp);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String clientIp) {
        log.debug("Password recovery initiated for: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString();
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(OffsetDateTime.now().plusMinutes(15));
            userRepository.save(user);

            log.info("PASSWORD RECOVERY COMPLETED. Cryptographic reset token for {} is: {}", request.getEmail(), resetToken);
            
            String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
            auditLogService.log(user.getId(), mainRole, "PASSWORD_RESET_REQUEST", "AUTH", "Password reset token generated", clientIp);
        } else {
            // Prevent profile mapping checks by logging but completing normally
            log.warn("Password reset requested for non-existent email address: {}", request.getEmail());
            auditLogService.log(null, "ANONYMOUS", "PASSWORD_RESET_REQUEST_INVALID_EMAIL", "AUTH", 
                    "Attempted password reset request for non-existent email: " + request.getEmail(), clientIp);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, String clientIp) {
        log.debug("Applying password reset via token");

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new DomainException("INVALID_RESET_TOKEN", "The password reset token is incorrect or invalid.", HttpStatus.BAD_REQUEST));

        if (user.getResetTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new DomainException("EXPIRED_RESET_TOKEN", "The password reset token has expired. Please initiate another recovery.", HttpStatus.BAD_REQUEST);
        }

        // Apply new credentials and wipe active reset keys
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.resetFailedLogins();
        
        // Ensure account is reactivated if it was suspended
        if (user.getStatus() == UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.ACTIVE);
        }
        
        userRepository.save(user);

        String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
        auditLogService.log(user.getId(), mainRole, "PASSWORD_RESET_SUCCESS", "AUTH", "Password overridden successfully", clientIp);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse refreshAccessToken(String refreshToken, String clientIp) {
        log.debug("Refreshing Access Token using Refresh JWT");

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new DomainException("INVALID_REFRESH_TOKEN", "The refresh token is invalid or expired.", HttpStatus.UNAUTHORIZED);
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("UNAUTHORIZED", "User profile not found.", HttpStatus.UNAUTHORIZED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException("UNAUTHORIZED", "Your account is not active. Status is: " + user.getStatus(), HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName().name())
                .collect(Collectors.toList());

        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail(), roles);
        
        auditLogService.log(user.getId(), roles.get(0), "ACCESS_TOKEN_REFRESH", "AUTH", "Access Token rotated successfully", clientIp);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("USER_NOT_FOUND", "User profile not found", HttpStatus.NOT_FOUND));
    }
}
