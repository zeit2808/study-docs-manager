package com.studydocs.manager.security.filter;

import com.studydocs.manager.security.jwt.JwtCookieService;
import com.studydocs.manager.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.FilterChain;
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
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final JwtCookieService jwtCookieService;
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, JwtCookieService jwtCookieService) {
        this.tokenProvider = tokenProvider;
        this.jwtCookieService = jwtCookieService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String cookieJwt = jwtCookieService.extractJwtFromCookie(request);
            String headerJwt = getJwtFromAuthorizationHeader(request);

            String jwt = null;

            if (StringUtils.hasText(cookieJwt) && tokenProvider.validateToken(cookieJwt)) {
                jwt = cookieJwt;
            } else if (StringUtils.hasText(headerJwt) && tokenProvider.validateToken(headerJwt)) {
                jwt = headerJwt;
            }

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                String roles = extractRolesFromToken(jwt);

                List<SimpleGrantedAuthority> authorities = Arrays.stream(
                                roles == null ? new String[0] : roles.split(","))
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

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromAuthorizationHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private String extractRolesFromToken(String token) {
        try {
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                    .verifyWith(tokenProvider.getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("roles", String.class);
        } catch (Exception e) {
            return "";
        }
    }
}
