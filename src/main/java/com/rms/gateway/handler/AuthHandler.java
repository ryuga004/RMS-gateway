package com.rms.gateway.handler;

import com.rms.gateway.dao.UserJpaRepository;
import com.rms.gateway.dto.LoginRequest;
import com.rms.gateway.dto.TokenResponse;
import com.rms.gateway.entity.UserEntity;
import com.rms.gateway.exception.NotFoundException;
import com.rms.gateway.exception.UnauthorizedException;
import com.rms.gateway.util.JwtUtil;
import com.rms.gateway.util.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthHandler {

    private final UserJpaRepository userRepo;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpirySeconds;

    /**
     * Login: find user by email in users table, validate password, issue tokens.
     */
    public TokenResponse login(LoginRequest request) {
        UserEntity user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("No account found with this email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        TokenResponse tokens = jwtUtil.generateTokens(
                user.getId(), user.getEmail(), user.getRoleId().intValue(), user.getJwtSecret());

        storeRefreshToken(tokens.getRefreshToken(), user.getId().toString());
        return tokens;
    }

    /**
     * Refresh: validate refresh token in Redis, load user, issue new token pair and rotate.
     */
    public TokenResponse refresh(String refreshToken) {
        String userId = redisTemplate.opsForValue().get(RedisKeyConstants.REFRESH_TOKEN_KEY_PREFIX + refreshToken);
        if (userId == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userLongId = Long.valueOf(userId);
        UserEntity user = userRepo.findById(userLongId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        TokenResponse newTokens = jwtUtil.generateTokens(
                user.getId(), user.getEmail(), user.getRoleId().intValue(), user.getJwtSecret());

        redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_KEY_PREFIX + refreshToken);
        storeRefreshToken(newTokens.getRefreshToken(), userId);
        return newTokens;
    }

    /**
     * Logout: remove refresh token from Redis.
     */
    public void logout(String refreshToken) {
        redisTemplate.delete(RedisKeyConstants.REFRESH_TOKEN_KEY_PREFIX + refreshToken);
    }

    private void storeRefreshToken(String refreshToken, String userId) {
        redisTemplate.opsForValue().set(
                RedisKeyConstants.REFRESH_TOKEN_KEY_PREFIX + refreshToken,
                userId,
                Duration.ofSeconds(refreshTokenExpirySeconds));
    }
}
