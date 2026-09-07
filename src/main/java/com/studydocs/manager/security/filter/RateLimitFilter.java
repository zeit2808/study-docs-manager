package com.studydocs.manager.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studydocs.manager.config.RateLimitProperties;
import com.studydocs.manager.dto.common.ErrorResponse;
import com.studydocs.manager.security.service.RateLimiterService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterService rateLimiterService,
                           RateLimitProperties props,
                           ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String clientIp = getClientIp(req);
        String key = clientIp + ":" + path;

        Integer limit = null;
        if (path.startsWith("/api/auth/login")) {
            limit = props.getLoginPerMinute();
        } else if (path.startsWith("/api/auth/register")) {
            limit = props.getRegisterPerMinute();
        } else if (path.startsWith("/api/auth/forgot-password")) {
            limit = props.getForgotPasswordPerMinute();
        } else if (path.startsWith("/api/auth/reset-password")) {
            limit = props.getResetPasswordPerMinute();
        }

        if (limit != null) {
            boolean allowed = rateLimiterService.tryConsume(key, limit);
            if (!allowed) {
                res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                res.setContentType("application/json;charset=UTF-8");

                ErrorResponse errorResponse = new ErrorResponse(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too Many Requests",
                        "Too many requests, please try again later.",
                        path
                );
                errorResponse.setCode("IP_RATE_LIMIT_EXCEEDED");
                errorResponse.setTimestamp(LocalDateTime.now());

                res.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
