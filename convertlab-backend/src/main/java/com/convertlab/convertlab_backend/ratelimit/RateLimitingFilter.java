package com.convertlab.convertlab_backend.ratelimit;

import com.convertlab.convertlab_backend.api.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Log4j2
public class RateLimitingFilter extends OncePerRequestFilter {

    private final IpRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RateLimitType type = resolveRateLimitType(request);

        if (type != null) {
            String clientIp = extractClientIp(request);

            if (!rateLimiter.allowRequest(clientIp, type)) {
                log.warn("Rate limit exceeded for {} on request type :{}", clientIp, type);
                writeRateLimitResponse(request, response);
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

        return null; // no rate limit
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {

        ApiResponse<Void> body = ApiResponse.failure(
                "Rate limit exceeded. Please try again later.",
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

