package com.studydocs.manager.security;

import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;


@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }


    public String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // Trong JwtAuthenticationFilter, principal đang là username (String)
        if (principal instanceof String) {
            return (String) principal;
        }
        return authentication.getName();
    }


    public Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null || username.isBlank()) {
            return null;
        }
        Optional<User> userOpt = userRepository.findByUsername(username);
        return userOpt.map(User::getId).orElse(null);
    }

    public HttpServletRequest getCurrentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }


    public String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // Nếu có nhiều IP (proxy chain) thì lấy IP đầu tiên
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String getUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }
}


