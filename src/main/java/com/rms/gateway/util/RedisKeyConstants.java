package com.rms.gateway.util;

import java.time.Duration;

/**
 * Redis key prefixes and TTLs used for JWT secret cache and refresh tokens.
 */
public final class RedisKeyConstants {

    public static final String JWT_SECRET_CACHE_PREFIX = "jwt_secret:";
    public static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
    public static final Duration JWT_SECRET_CACHE_TTL = Duration.ofHours(24);

    private RedisKeyConstants() {
    }
}
