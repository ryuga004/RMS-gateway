package com.rms.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rms.gateway.dao.UserJpaRepository;
import com.rms.gateway.dto.ApiResponse;
import com.rms.gateway.util.AuthConstants;
import com.rms.gateway.util.GatewayPathConstants;
import com.rms.gateway.util.JwtUtil;
import com.rms.gateway.util.RedisKeyConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserJpaRepository userRepo;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = extractAccessToken(request);
        if (token == null) {
            sendUnauthorized(request, response, "Missing or invalid authentication");
            return;
        }

        try {
            Long userId = jwtUtil.extractUserIdUnsafe(token);
            String jwtSecret = resolveJwtSecret(userId);
            if (jwtSecret == null) {
                sendUnauthorized(request, response, "User not found");
                return;
            }

            Claims claims = jwtUtil.validateToken(token, jwtSecret);
            setAuthenticationContext(userId, claims);
            HttpServletRequest requestToForward = ensureAuthorizationHeader(request, token);
            filterChain.doFilter(requestToForward, response);

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            sendUnauthorized(request, response, "Invalid or expired token");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return GatewayPathConstants.PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Extract access token from cookie only (client uses cookie-based auth).
     */
    private String extractAccessToken(HttpServletRequest request) {
        return getAccessTokenFromCookie(request);
    }

    private String getAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> AuthConstants.ACCESS_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .filter(v -> !v.isBlank())
                .orElse(null);
    }

    private String resolveJwtSecret(Long userId) {
        String cacheKey = RedisKeyConstants.JWT_SECRET_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        return userRepo.findById(userId).map(user -> {
            redisTemplate.opsForValue().set(cacheKey, user.getJwtSecret(), RedisKeyConstants.JWT_SECRET_CACHE_TTL);
            return user.getJwtSecret();
        }).orElse(null);
    }

    private void setAuthenticationContext(Long userId, Claims claims) {
        Integer roleId = claims.get("roleId", Integer.class);
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + roleId)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Ensure downstream receives token in Authorization header (Bearer).
     * If client sent cookie only, wrap request and add the header.
     */
    private HttpServletRequest ensureAuthorizationHeader(HttpServletRequest request, String token) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            return request;
        }
        var mutableRequest = new HeaderMutableHttpServletRequest(request);
        mutableRequest.addHeader("Authorization", AuthConstants.BEARER_PREFIX + token);
        return mutableRequest;
    }

    private void sendUnauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        addCorsHeadersToErrorResponse(request, response);
        ApiResponse<Void> body = ApiResponse.error("UNAUTHORIZED", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void addCorsHeadersToErrorResponse(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }
}
