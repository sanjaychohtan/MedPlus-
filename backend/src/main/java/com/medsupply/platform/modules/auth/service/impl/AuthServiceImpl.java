package com.medsupply.platform.modules.auth.service.impl;

import com.medsupply.platform.common.exception.DomainException;
import com.medsupply.platform.modules.audit.service.AuditLogService;
import com.medsupply.platform.modules.auth.dto.*;
import com.medsupply.platform.modules.auth.model.*;
import com.medsupply.platform.modules.auth.repository.RoleRepository;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.auth.security.JwtTokenProvider;
import com.medsupply.platform.modules.auth.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise-grade, hardened Service implementation managing user registration, logins,
 * OTP verification, password resets, and session refreshes.
 * Extends the platform security ledger with absolute transaction safety and zero in-memory states.
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
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Lazy
    private AuthServiceImpl self;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final String ROLE_PREFIX = "ROLE_";

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.otp.length:6}")
    private int otpLength;

    @Value("${app.security.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.security.otp.max-attempts:3}")
    private int otpMaxAttempts;

    @Value("${app.security.otp.cooldown-seconds:60}")
    private int otpCooldownSeconds;

    private String getPrefixedRole(UserRole role) {
        return ROLE_PREFIX + role.name();
    }

    private String getClientDeviceInfo() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userAgent = request.getHeader("User-Agent");
            String fingerprint = request.getHeader("X-Device-Fingerprint");
            return "UA: " + (userAgent != null ? userAgent : "Unknown") + 
                   " | Fingerprint: " + (fingerprint != null ? fingerprint : "Unknown");
        }
        return "UA: Unknown | Fingerprint: Unknown";
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        String formatSpecifier = "%0" + otpLength + "d";
        return String.format(formatSpecifier, secureRandom.nextInt(bound));
    }

    /**
     * Hashes tokens or OTP codes using a cryptographically secure SHA-256 process before saving.
     */
    private String hashToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Critical cryptographic algorithm SHA-256 is unavailable", e);
        }
    }

    /**
     * Performs constant-time secure comparisons for OTP or reset validation strings.
     */
    private boolean safeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void validateAndUpdatePasswordHistory(User user, String newRawPassword) {
        String key = "password_history:" + user.getId().toString();
        List<Object> historyObjects = redisTemplate.opsForList().range(key, 0, -1);
        List<String> history = new ArrayList<>();
        if (historyObjects != null) {
            for (Object obj : historyObjects) {
                history.add((String) obj);
            }
        }
        
        // Check if the new password matches any in history (last 5)
        for (String oldHash : history) {
            if (passwordEncoder.matches(newRawPassword, oldHash)) {
                throw new DomainException("PASSWORD_REUSED", "The new password cannot be one of your last 5 passwords.", HttpStatus.BAD_REQUEST);
            }
        }
        
        // Check against current active password
        if (passwordEncoder.matches(newRawPassword, user.getPasswordHash())) {
            throw new DomainException("PASSWORD_REUSED", "The new password cannot be the same as your current password.", HttpStatus.BAD_REQUEST);
        }

        // Add current hash to history list
        redisTemplate.opsForList().rightPush(key, user.getPasswordHash());
        redisTemplate.opsForList().trim(key, -5, -1);
    }

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
                        "Login blocked: Account is pending OTP approval [" + getClientDeviceInfo() + "]", clientIp);
                throw new DomainException("PENDING_APPROVAL", "Your account is pending verification. Please verify your OTP code.", HttpStatus.FORBIDDEN);
            }
            if (user.getStatus() == UserStatus.SUSPENDED) {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_BLOCKED_SUSPENDED", "AUTH", 
                        "Login blocked: Account is suspended due to too many failed attempts [" + getClientDeviceInfo() + "]", clientIp);
                throw new DomainException("ACCOUNT_SUSPENDED", "Your account has been suspended. Please contact customer service.", HttpStatus.FORBIDDEN);
            }
            if (user.getStatus() == UserStatus.DEACTIVATED) {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_BLOCKED_DEACTIVATED", "AUTH", 
                        "Login blocked: Account is deactivated [" + getClientDeviceInfo() + "]", clientIp);
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

            // Write success trace to global security audit log with full device context
            String primaryRole = roles.isEmpty() ? "ANONYMOUS" : roles.get(0);
            auditLogService.log(user.getId(), primaryRole, "LOGIN_SUCCESS", "AUTH", 
                    "User successfully authenticated [" + getClientDeviceInfo() + "]", clientIp);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .roles(roles)
                    .build();

        } catch (BadCredentialsException ex) {
            // Handle and throttle failed login attempts inside a separate REQUIRES_NEW transaction
            self.handleFailedLoginRequiresNew(request.getEmail(), clientIp);
            throw ex;
        }
    }

    /**
     * Increments the user's failed login attempt counter and updates the user's status 
     * to SUSPENDED if max attempts are exceeded. Runs inside an independent transaction (REQUIRES_NEW)
     * so that the changes survive login transaction rollback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedLoginRequiresNew(String email, String clientIp) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.incrementFailedLogins();
            
            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setStatus(UserStatus.SUSPENDED);
                log.warn("User account {} has been SUSPENDED due to {} consecutive failed login attempts.", email, maxFailedAttempts);
                auditLogService.log(user.getId(), "ANONYMOUS", "ACCOUNT_AUTO_LOCKOUT", "AUTH", 
                        "Account suspended automatically due to " + maxFailedAttempts + " failed attempts [" + getClientDeviceInfo() + "]", clientIp);
            } else {
                auditLogService.log(user.getId(), "ANONYMOUS", "LOGIN_ATTEMPT_FAILED", "AUTH", 
                        "Failed login attempt #" + user.getFailedLoginAttempts() + " [" + getClientDeviceInfo() + "]", clientIp);
            }
            userRepository.save(user);
        } else {
            auditLogService.log(null, "ANONYMOUS", "LOGIN_ATTEMPT_INVALID_USER", "AUTH", 
                    "Attempted login with non-existent email: " + email + " [" + getClientDeviceInfo() + "]", clientIp);
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

        // Generate configured numeric verification OTP code
        String otpCode = generateOtpCode();
        OffsetDateTime otpExpiry = OffsetDateTime.now().plusMinutes(otpExpiryMinutes);

        // Store secure cryptographic hash of the OTP
        String hashedOtp = hashToken(otpCode);

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .licenseNumber(request.getLicenseNumber())
                .gstin(request.getGstin())
                .status(UserStatus.PENDING_APPROVAL)
                .otpCode(hashedOtp)
                .otpExpiry(otpExpiry)
                .roles(new HashSet<>(Collections.singletonList(role)))
                .build();

        userRepository.save(user);
        
        // Reset failed OTP and resend trackers in Redis for a clean slate
        redisTemplate.delete("otp_failed_attempts:" + request.getEmail());
        redisTemplate.delete("otp_last_resend:" + request.getEmail());

        // Log secure verification output with OTP masked/available for dev debugging
        log.info("REGISTRATION COMPLETED. Verification OTP sent for email: {} (Hashed: {})", request.getEmail(), hashedOtp);
        log.debug("DEV MODE: Plaintext OTP code: {}", otpCode);

        auditLogService.log(user.getId(), "ANONYMOUS", "USER_REGISTRATION", "AUTH", 
                "Registered new user account in state PENDING_APPROVAL with role: " + role.getName() + " [" + getClientDeviceInfo() + "]", clientIp);
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

        // Prevent OTP verification if maximum attempts exceeded (brute-force protection)
        String attemptsKey = "otp_failed_attempts:" + request.getEmail();
        String attemptsVal = (String) redisTemplate.opsForValue().get(attemptsKey);
        int attempts = (attemptsVal != null) ? Integer.parseInt(attemptsVal) : 0;
        
        if (attempts >= otpMaxAttempts) {
            log.warn("OTP verification blocked for user {} due to too many failed attempts ({} attempts).", request.getEmail(), attempts);
            auditLogService.log(user.getId(), "ANONYMOUS", "OTP_VERIFICATION_BLOCKED", "AUTH", 
                    "OTP verification blocked due to max failed attempts [" + getClientDeviceInfo() + "]", clientIp);
            throw new DomainException("OTP_LOCKED", "Verification is locked due to too many incorrect attempts. Please resend a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
        }

        if (user.getOtpCode() == null || !safeEquals(user.getOtpCode(), hashToken(request.getOtp()))) {
            int newAttempts = self.handleFailedOtpAttemptRequiresNew(request.getEmail(), user.getId(), clientIp);

            if (newAttempts >= otpMaxAttempts) {
                throw new DomainException("OTP_LOCKED", "Verification is locked due to too many incorrect attempts. Please resend a new OTP.", HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new DomainException("INVALID_OTP", "The verification code is incorrect. Remaining attempts: " + (otpMaxAttempts - newAttempts), HttpStatus.BAD_REQUEST);
        }

        if (user.getOtpExpiry().isBefore(OffsetDateTime.now())) {
            throw new DomainException("EXPIRED_OTP", "The verification code has expired. Please request a new code.", HttpStatus.BAD_REQUEST);
        }

        // Activate account
        user.setStatus(UserStatus.ACTIVE);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Reset failed OTP tracking upon success
        redisTemplate.delete(attemptsKey);

        String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
        auditLogService.log(user.getId(), mainRole, "OTP_VERIFICATION_SUCCESS", "AUTH", 
                "Account successfully activated [" + getClientDeviceInfo() + "]", clientIp);
    }

    /**
     * Increments the OTP failed attempt counter in Redis and, if limit is exceeded, invalidates
     * the OTP record in the database. Executed in a REQUIRES_NEW transaction to survive verification failures.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int handleFailedOtpAttemptRequiresNew(String email, UUID userId, String clientIp) {
        String key = "otp_failed_attempts:" + email;
        Long countObj = redisTemplate.opsForValue().increment(key);
        int attempts = countObj != null ? countObj.intValue() : 1;
        redisTemplate.expire(key, Duration.ofDays(1));
        
        auditLogService.log(userId, "ANONYMOUS", "OTP_VERIFICATION_FAILED", "AUTH", 
                "Incorrect OTP entered. Attempt " + attempts + " of " + otpMaxAttempts + " [" + getClientDeviceInfo() + "]", clientIp);

        if (attempts >= otpMaxAttempts) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setOtpCode(null);
                user.setOtpExpiry(null);
                userRepository.save(user);
            }
            auditLogService.log(userId, "ANONYMOUS", "OTP_VERIFICATION_BLOCKED", "AUTH", 
                    "OTP verification locked/invalidated due to max failed attempts [" + getClientDeviceInfo() + "]", clientIp);
        }
        return attempts;
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

        // OTP resend cooldown (60 seconds, configurable)
        String resendKey = "otp_last_resend:" + email;
        String lastResendVal = (String) redisTemplate.opsForValue().get(resendKey);
        if (lastResendVal != null) {
            OffsetDateTime lastResend = OffsetDateTime.parse(lastResendVal);
            long remainingSeconds = java.time.Duration.between(OffsetDateTime.now(), lastResend.plusSeconds(otpCooldownSeconds)).getSeconds();
            if (remainingSeconds > 0) {
                log.warn("OTP resend rate limited for {}. Cooldown remaining: {} seconds", email, remainingSeconds);
                throw new DomainException("OTP_COOLDOWN", "Please wait " + remainingSeconds + " seconds before requesting another code.", HttpStatus.TOO_MANY_REQUESTS);
            }
        }

        // Generate and configure new OTP
        String otpCode = generateOtpCode();
        log.info("DEBUG: Generated fresh OTP for resend: {} (Hashed: {})", email, hashToken(otpCode));
        user.setOtpCode(hashToken(otpCode));
        user.setOtpExpiry(OffsetDateTime.now().plusMinutes(otpExpiryMinutes));
        userRepository.save(user);

        // Update tracking and reset verification failed attempts
        redisTemplate.opsForValue().set(resendKey, OffsetDateTime.now().toString(), Duration.ofSeconds(otpCooldownSeconds));
        redisTemplate.delete("otp_failed_attempts:" + email);

        log.info("RESENT OTP COMPLETED. Fresh verification OTP sent for email: {}", email);

        String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
        auditLogService.log(user.getId(), mainRole, "OTP_RESEND", "AUTH", 
                "OTP code regenerated and sent [" + getClientDeviceInfo() + "]", clientIp);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String clientIp) {
        log.debug("Password recovery initiated for: {}", request.getEmail());

        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String resetToken = UUID.randomUUID().toString();
            
            // Store secure cryptographic hash of the reset token
            String hashedResetToken = hashToken(resetToken);
            user.setResetToken(hashedResetToken);
            user.setResetTokenExpiry(OffsetDateTime.now().plusMinutes(15));
            userRepository.save(user);

            log.info("PASSWORD RECOVERY COMPLETED. Password reset token generated for: {}", request.getEmail());
            log.debug("DEV MODE: Plaintext reset token: {}", resetToken);
            
            String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
            auditLogService.log(user.getId(), mainRole, "PASSWORD_RESET_REQUEST", "AUTH", 
                    "Password reset token generated [" + getClientDeviceInfo() + "]", clientIp);
        } else {
            // Prevent profile mapping checks by logging but completing normally (protection against user enumeration)
            log.warn("Password reset requested for non-existent email address: {}", request.getEmail());
            auditLogService.log(null, "ANONYMOUS", "PASSWORD_RESET_REQUEST_INVALID_EMAIL", "AUTH", 
                    "Attempted password reset request for non-existent email: " + request.getEmail() + " [" + getClientDeviceInfo() + "]", clientIp);
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, String clientIp) {
        log.debug("Applying password reset via token");

        // Hash the incoming plaintext reset token to search the database
        String hashedIncomingToken = hashToken(request.getToken());

        User user = userRepository.findByResetToken(hashedIncomingToken)
                .orElseThrow(() -> new DomainException("INVALID_RESET_TOKEN", "The password reset token is incorrect or invalid.", HttpStatus.BAD_REQUEST));

        if (user.getResetTokenExpiry().isBefore(OffsetDateTime.now())) {
            throw new DomainException("EXPIRED_RESET_TOKEN", "The password reset token has expired. Please initiate another recovery.", HttpStatus.BAD_REQUEST);
        }

        // Password history validation (last 5 passwords cannot be reused)
        validateAndUpdatePasswordHistory(user, request.getNewPassword());

        // Apply new credentials and wipe active reset keys
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.resetFailedLogins();
        
        // Note: Suspended users remain suspended to prevent automated reactivation bypasses
        userRepository.save(user);

        // Global token invalidation on password reset
        try {
            String epochKey = "user_revocation_epoch:" + user.getEmail();
            redisTemplate.opsForValue().set(epochKey, String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Failed to set user revocation epoch in Redis on password reset", e);
        }

        String mainRole = user.getRoles().isEmpty() ? "B2C_CUSTOMER" : user.getRoles().iterator().next().getName().name();
        auditLogService.log(user.getId(), mainRole, "PASSWORD_RESET_SUCCESS", "AUTH", 
                "Password overridden successfully [" + getClientDeviceInfo() + "]", clientIp);
    }

    @Override
    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken, String clientIp) {
        log.debug("Refreshing Access Token using Refresh JWT");

        try {
            tokenProvider.validateToken(refreshToken);
            String tokenType = tokenProvider.getTokenTypeFromToken(refreshToken);
            if (!"refresh".equals(tokenType)) {
                throw new DomainException("INVALID_REFRESH_TOKEN", "The token type is invalid.", HttpStatus.UNAUTHORIZED);
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new DomainException("INVALID_REFRESH_TOKEN", "The refresh token is invalid or expired.", HttpStatus.UNAUTHORIZED);
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("UNAUTHORIZED", "User profile not found.", HttpStatus.UNAUTHORIZED));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException("UNAUTHORIZED", "Your account is not active. Status is: " + user.getStatus(), HttpStatus.UNAUTHORIZED);
        }

        // Detect attempt to reuse a revoked refresh token (true refresh token rotation security)
        String revokedKey = "revoked_refresh_token:" + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(revokedKey))) {
            log.warn("Revoked refresh token reuse attempt detected for user: {}", email);
            auditLogService.log(user.getId(), "ANONYMOUS", "REFRESH_TOKEN_REUSE_ATTEMPT", "AUTH", 
                    "Attempted reuse of revoked refresh token [" + getClientDeviceInfo() + "]", clientIp);
            throw new DomainException("INVALID_REFRESH_TOKEN", "The refresh token has been revoked.", HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = user.getRoles().stream()
                .map(role -> getPrefixedRole(role.getName()))
                .collect(Collectors.toList());

        // Perform rotation: Generate a new access token AND a new refresh token
        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail(), roles);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // Revoke the old refresh token (7 days default lifetime)
        redisTemplate.opsForValue().set(revokedKey, "revoked", Duration.ofDays(7));
        
        String primaryRole = roles.isEmpty() ? "ANONYMOUS" : roles.get(0);
        auditLogService.log(user.getId(), primaryRole, "REFRESH_TOKEN_ROTATED", "AUTH", 
                "Access and Refresh Tokens rotated successfully [" + getClientDeviceInfo() + "]", clientIp);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
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

    @Override
    @Transactional
    public void logout(String refreshToken, String clientIp) {
        log.debug("Logging out user, revoking refresh token and JTI");
        try {
            if (tokenProvider.validateToken(refreshToken)) {
                String email = tokenProvider.getEmailFromToken(refreshToken);
                User user = userRepository.findByEmail(email).orElse(null);
                
                // Revoke the refresh token value itself
                String revokedKey = "revoked_refresh_token:" + refreshToken;
                redisTemplate.opsForValue().set(revokedKey, "revoked", Duration.ofDays(7));
                
                // Extract JTI and revoke it
                String jti = tokenProvider.getJtiFromToken(refreshToken);
                if (jti != null) {
                    redisTemplate.opsForValue().set("revoked_jti:" + jti, "revoked", Duration.ofDays(7));
                }

                if (user != null) {
                    // Global token invalidation for this specific session or force next tokens to be newer
                    String epochKey = "user_revocation_epoch:" + user.getEmail();
                    redisTemplate.opsForValue().set(epochKey, String.valueOf(System.currentTimeMillis()));

                    String primaryRole = user.getRoles().isEmpty() ? "ANONYMOUS" : user.getRoles().iterator().next().getName().name();
                    auditLogService.log(user.getId(), primaryRole, "LOGOUT_SUCCESS", "AUTH", 
                            "User logged out, JTI (" + jti + ") and refresh token revoked successfully [" + getClientDeviceInfo() + "]", clientIp);
                }
            }
        } catch (Exception e) {
            log.error("Error during token revocation on logout: {}", e.getMessage());
        }
    }
}
