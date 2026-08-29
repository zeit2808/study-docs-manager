package com.studydocs.manager.security.csrf;

import com.studydocs.manager.config.JwtCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

public class CookieJwtCsrfRequestMatcher implements RequestMatcher {

    private final JwtCookieProperties jwtCookieProperties;

    public CookieJwtCsrfRequestMatcher(JwtCookieProperties jwtCookieProperties) {
        this.jwtCookieProperties = jwtCookieProperties;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String method = request.getMethod();

        // GET/HEAD/OPTIONS/TRACE không thay đổi dữ liệu.
        if (HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method)
                || HttpMethod.TRACE.matches(method)) {
            return false;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        // Chỉ bắt CSRF nếu JWT được gửi qua cookie.
        // Bearer token cho Swagger/Postman/Mobile sẽ không bị ảnh hưởng.
        return Arrays.stream(cookies)
                .anyMatch(cookie ->
                        jwtCookieProperties.getName().equals(cookie.getName()));
    }
}