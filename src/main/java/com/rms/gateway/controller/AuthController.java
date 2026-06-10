package com.rms.gateway.controller;

import com.rms.gateway.dto.ApiResponse;
import com.rms.gateway.dto.LoginRequest;
import com.rms.gateway.util.AuthConstants;
import com.rms.gateway.dto.RefreshRequest;
import com.rms.gateway.dto.TokenResponse;
import com.rms.gateway.handler.AuthHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthHandler authHandler;

    @Value("${jwt.access-token-expiry}")
    private int accessTokenExpirySeconds;

    @Value("${jwt.refresh-token-expiry}")
    private int refreshTokenExpirySeconds;

    @Value("${app.cookie.same-site:}")
    private String cookieSameSite;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.path:/}")
    private String cookiePath;

    @Value("${app.cookie.http-only:true}")
    private boolean cookieHttpOnly;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        TokenResponse tokens = authHandler.login(request);
        appendAuthCookies(response, tokens);
        return ResponseEntity.ok(ApiResponse.success(null, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String refreshToken = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                ? request.getRefreshToken()
                : getRefreshTokenFromCookie(httpRequest);
        if (refreshToken == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.toString(), "Refresh token required (cookie or body)"));
        }
        TokenResponse tokens = authHandler.refresh(refreshToken);
        appendAuthCookies(response, tokens);
        return ResponseEntity.ok(ApiResponse.success(null, "Tokens refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            authHandler.logout(refreshToken);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    private void appendAuthCookies(HttpServletResponse response, TokenResponse tokens) {
        appendSetCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, tokens.getAccessToken(), accessTokenExpirySeconds);
        appendSetCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, tokens.getRefreshToken(), refreshTokenExpirySeconds);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        appendSetCookie(response, AuthConstants.ACCESS_TOKEN_COOKIE_NAME, "", 0);
        appendSetCookie(response, AuthConstants.REFRESH_TOKEN_COOKIE_NAME, "", 0);
    }

    private void appendSetCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path(cookiePath);
        if (maxAgeSeconds == 0) {
            builder.maxAge(Duration.ZERO);
        } else {
            builder.maxAge(Duration.ofSeconds(maxAgeSeconds));
        }
        if (cookieSameSite != null && !cookieSameSite.isBlank()) {
            builder.sameSite(cookieSameSite.trim());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> AuthConstants.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}
