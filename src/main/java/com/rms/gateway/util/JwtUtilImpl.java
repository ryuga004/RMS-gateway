package com.rms.gateway.util;

import com.rms.gateway.dto.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtilImpl implements JwtUtil {

    @Value("${jwt.access-token-expiry}")
    private long accessTokenExpirySeconds;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpirySeconds;

    @Override
    public TokenResponse generateTokens(Long userId, String email, int roleId, String jwtSecret) {
        SecretKey key = buildKey(jwtSecret);
        long now = System.currentTimeMillis();

        String accessToken = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roleId", roleId)
                .claim("userId", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirySeconds * 1000))
                .signWith(key)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenExpirySeconds * 1000))
                .signWith(key)
                .compact();

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    public Claims validateToken(String token, String jwtSecret) {
        return Jwts.parser()
                .verifyWith(buildKey(jwtSecret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public Long extractUserIdUnsafe(String token) {
        // Extract subject without verification — only to retrieve the per-tenant secret
        String payload = new String(
                java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8);
        // Simple JSON parsing to extract "sub" field
        int subIndex = payload.indexOf("\"sub\":\"");
        if (subIndex == -1)
            throw new IllegalArgumentException("Token missing subject");
        int start = subIndex + 7;
        int end = payload.indexOf("\"", start);
        return Long.valueOf(payload.substring(start, end));
    }

    private SecretKey buildKey(String secret) {
        // Pad or hash secret to ensure minimum 256-bit key length
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
