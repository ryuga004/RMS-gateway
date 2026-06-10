package com.rms.gateway.util;

/**
 * Auth-related constants: Bearer prefix and cookie names for access/refresh tokens.
 */
public final class AuthConstants {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "rms_access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "rms_refresh_token";

    private AuthConstants() {
    }
}
