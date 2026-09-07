package com.studydocs.manager.security.filter;

import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.jwt.JwtCookieService;
import com.studydocs.manager.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final JwtCookieService jwtCookieService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            JwtCookieService jwtCookieService,
            UserRepository userRepository
    ) {
        this.tokenProvider = tokenProvider;
        this.jwtCookieService = jwtCookieService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = resolveToken(request);

            if (StringUtils.hasText(jwt)
                    && tokenProvider.validateToken(jwt)
                    && SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                authenticateRequest(jwt, request);
            }
        } catch (Exception ex) {
            logger.warn("JWT authentication failed", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String cookieJwt = jwtCookieService.extractJwtFromCookie(request);

        if (StringUtils.hasText(cookieJwt)
                && tokenProvider.validateToken(cookieJwt)) {
            return cookieJwt;
        }

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    private void authenticateRequest(
            String jwt,
            HttpServletRequest request
    ) {
        String username = tokenProvider.getUsernameFromToken(jwt);

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return;
        }

        long tokenVersion = tokenProvider.getTokenVersionFromToken(jwt);

        // JWT cũ, JWT không có claim, hoặc JWT đã bị revoke sau đổi password.
        if (tokenVersion != user.getTokenVersion()) {
            return;
        }

        String roles = extractRolesFromToken(jwt);

        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles.split(","))
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    private String extractRolesFromToken(String token) {
        try {
            return io.jsonwebtoken.Jwts.parser()
                    .verifyWith(tokenProvider.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("roles", String.class);
        } catch (Exception ex) {
            return "";
        }
    }
}
