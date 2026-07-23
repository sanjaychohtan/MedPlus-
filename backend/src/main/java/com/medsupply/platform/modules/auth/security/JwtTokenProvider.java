package com.medsupply.platform.modules.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles security computations for JSON Web Tokens (JWT).
 * Signs, parses, validates, and rotates Access and Refresh tokens.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;
    private final JwtParser jwtParser;

    public JwtTokenProvider(
            @Value("${app.security.jwt-secret}") String jwtSecret,
            @Value("${app.security.jwt-expiration-ms}") long jwtExpirationMs,
            @Value("${app.security.refresh-token-expiration-ms}") long refreshExpirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        
        // Clock skew of 60 seconds handled during parsing
        this.jwtParser = Jwts.parser()
                .verifyWith(this.key)
                .clockSkewSeconds(60)
                .build();
    }

    /**
     * Creates a signed 15-minute Access Token storing user email and roles.
     */
    public String generateAccessToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("type", "access")
                .claim("iss", "medsupply-platform")
                .claim("aud", "medsupply-clients")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Direct string overload for creating Access Token.
     */
    public String generateAccessToken(String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .claim("type", "access")
                .claim("iss", "medsupply-platform")
                .claim("aud", "medsupply-clients")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Generates a secure, cryptographically robust 7-day Refresh Token.
     */
    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .claim("iss", "medsupply-platform")
                .claim("aud", "medsupply-clients")
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Decodes and extracts the user email from a verified JWT token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    /**
     * Decodes and extracts the user roles from a verified JWT token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.get("roles", List.class);
    }

    /**
     * Extracts token type claim.
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        return claims.get("type", String.class);
    }

    /**
     * Validates a token signature, parsing integrity and expiration times.
     */
    public boolean validateToken(String authToken) throws JwtException, IllegalArgumentException {
        jwtParser.parseSignedClaims(authToken);
        return true;
    }
}
