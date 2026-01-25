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

        chain.doFilter(req, res);

        String allowedOrigin = response.getHeader("Access-Control-Allow-Origin");
        if (!origin.equals(allowedOrigin)) {
            log.error("CORS request - Origin: {}, Method: {}, URI: {}",
                    origin, method, request.getRequestURI());
            log.error("CORS response - Allowed Origin: {}", allowedOrigin);
        }
    }

}