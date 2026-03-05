package com.convertlab.convertlab_backend.ratelimit;

import com.convertlab.convertlab_backend.api.ApiResponse;
import com.convertlab.convertlab_backend.service_util.IpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip Rate Limiting for CORS pre-flight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitType type = resolveRateLimitType(request);

        if (type != null) {
            String clientIp = IpUtil.extractClientIp(request);

            if (!rateLimiter.allowRequest(clientIp, type)) {
                log.warn("Rate limit exceeded for {} on request type: {}", clientIp, type);
                writeRateLimitResponse(request, response, clientIp, type);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private RateLimitType resolveRateLimitType(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path.startsWith("/api/upload")) {
            return RateLimitType.UPLOAD;
        }
        if (path.startsWith("/api/pdf") || path.startsWith("/api/image")) {
            return RateLimitType.ACTION;
        }
        if (path.startsWith("/api/auth/signup")) {
            return RateLimitType.SIGNUP;
        }
        if (path.startsWith("/api/documents/ingest")) {
            return RateLimitType.AI_INGEST;
        }
        if (path.startsWith("/api/documents/query")) {
            return RateLimitType.AI_QUERY;
        }

        return null; // no rate limit
    }

    private void writeRateLimitResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String ip,
            RateLimitType type
    ) throws IOException {
        TokenBucket bucket = rateLimiter.getTokenBucket(ip, type);
        ApiResponse<Void> body = ApiResponse.failure(
                "Rate limit exceeded. Please try after " + bucket.getRetryAfterTimeInSec() + "s",
                "RATE_LIMIT_EXCEEDED"
        );

        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), body);
    }
}