// RateLimitFilter.java
package com.studydocs.manager.security;

import com.studydocs.manager.config.RateLimitProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties props;

    public RateLimitFilter(RateLimiterService rateLimiterService,
                           RateLimitProperties props) {
        this.rateLimiterService = rateLimiterService;
        this.props = props;
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
        }

        if (limit != null) {
            boolean allowed = rateLimiterService.tryConsume(key, limit);
            if (!allowed) {
                res.setStatus(429);
                res.getWriter().write("Too many requests, please try again later.");
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