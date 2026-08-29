package com.studydocs.manager.security.jwt;
import com.studydocs.manager.config.JwtCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
@Service
public class JwtCookieService {
    private final JwtCookieProperties properties;

    public JwtCookieService(JwtCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie generateJwtCookie(String token, long expirationMillis) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.getName(), token)
                .httpOnly(true)
                .secure(properties.getSecure())
                .path(properties.getPath())
                .sameSite(properties.getSameSite())
                .maxAge(Duration.ofMillis(expirationMillis));

        if (StringUtils.hasText(properties.getDomain())) {
            builder.domain(properties.getDomain());
        }

        return builder.build();
    }

    public ResponseCookie getCleanJwtCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.getName(), "")
                .httpOnly(true)
                .secure(properties.getSecure())
                .path(properties.getPath())
                .sameSite(properties.getSameSite())
                .maxAge(Duration.ZERO);

        if (StringUtils.hasText(properties.getDomain())) {
            builder.domain(properties.getDomain());
        }

        return builder.build();
    }

    public String extractJwtFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> properties.getName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
