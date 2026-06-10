package com.rms.gateway.util;

import com.rms.gateway.dto.TokenResponse;
import io.jsonwebtoken.Claims;

/**
 * Contract for JWT generation and validation.
 * All methods are per-tenant: the jwtSecret is specific to each user's record.
 */
public interface JwtUtil {

    /**
     * Generate an access + refresh token pair for the given principal.
     */
    TokenResponse generateTokens(Long userId, String email, int roleId, String jwtSecret);

    /**
     * Parse and validate a token using the given secret.
     * Throws JwtException if invalid or expired.
     */
    Claims validateToken(String token, String jwtSecret);

    /**
     * Extract userId (subject) from a token WITHOUT signature verification.
     * Used only to look up the jwtSecret from Redis/DB before full validation.
     */
    Long extractUserIdUnsafe(String token);
}
