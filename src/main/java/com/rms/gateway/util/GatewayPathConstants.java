package com.rms.gateway.util;

import java.util.List;

/**
 * Path prefixes that skip JWT auth (filter and Security permitAll).
 * Single source of truth for gateway public paths.
 */
public final class GatewayPathConstants {

        /**
         * Path prefixes for which the JWT filter is skipped (e.g.
         * path.startsWith(prefix)).
         */
        public static final List<String> PUBLIC_PATH_PREFIXES = List.of(
                        "/auth/",
                        "/api/admin/register",
                        "/actuator");

        /** Ant-style patterns for SecurityConfig requestMatchers (permitAll). */
        public static final List<String> PERMIT_ALL_ANT_PATTERNS = List.of(
                        "/auth/**",
                        "/api/admin/register/**",
                        "/actuator/**",
                        "/error/**");

        private GatewayPathConstants() {
        }
}
