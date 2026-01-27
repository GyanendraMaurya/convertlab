package com.convertlab.convertlab_backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Log4j2
public class CorsLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        String method = request.getMethod();
        String uri = request.getRequestURI();

        chain.doFilter(req, res);

        // No Origin header → not a CORS request → ignore
        if (origin == null) {
            return;
        }

        if (response.getStatus() == 429) {
            return;
        }

        String allowedOrigin =
                response.getHeader("Access-Control-Allow-Origin");

        // CASE 1: CORS headers missing completely
        if (allowedOrigin == null) {
            log.warn("""
                    CORS BLOCKED
                    Origin: {}
                    Method: {}
                    URI: {}
                    Reason: Access-Control-Allow-Origin header missing
                    """, origin, method, uri);
            return;
        }

        // CASE 2: Origin present but not allowed
        if (!origin.equals(allowedOrigin)) {
            log.warn("""
                    CORS MISMATCH
                    Origin: {}
                    Allowed-Origin: {}
                    Method: {}
                    URI: {}
                    """, origin, allowedOrigin, method, uri);
        }
    }
}

