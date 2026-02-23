package com.ecom.common.web;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs every API request with method, path, status, and duration.
 * Add common-web dependency to services that need request logging.
 * Registered via RequestLoggingFilterConfig (auto-configuration).
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String correlationId = Optional.ofNullable(request.getHeader(CORRELATION_ID_HEADER)).orElse("-");
        String clientIp = Optional.ofNullable(request.getHeader(X_FORWARDED_FOR))
                .map(h -> h.contains(",") ? h.substring(0, h.indexOf(',')).trim() : h.trim())
                .orElseGet(() -> Optional.ofNullable(request.getRemoteAddr()).orElse("unknown"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            logger.info("api method={} path={} status={} durationMs={} correlationId={} clientIp={}",
                    method, path, status, duration, correlationId, clientIp);
        }
    }
}
