package com.studydocs.manager.controller.auth;

import com.studydocs.manager.application.auth.AuthApplicationService;
import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.dto.auth.JwtResponse;
import com.studydocs.manager.dto.auth.LoginRequest;
import com.studydocs.manager.dto.auth.LoginResponse;
import com.studydocs.manager.dto.auth.RegisterRequest;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.security.jwt.JwtCookieService;
import com.studydocs.manager.security.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final JwtCookieService jwtCookieService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            AuthApplicationService authApplicationService,
            JwtCookieService jwtCookieService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.authApplicationService = authApplicationService;
        this.jwtCookieService = jwtCookieService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Dành cho Browser / SPA.
     * JWT chỉ nằm trong HttpOnly cookie, không trả token trong body.
     */
    @PostMapping("/login")
    @Operation(
            summary = "Browser login",
            description = "Authenticate user and set JWT in an HttpOnly cookie"
    )
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        JwtResponse authResult = authApplicationService.login(loginRequest);

        ResponseCookie jwtCookie = jwtCookieService.generateJwtCookie(
                authResult.getToken(),
                jwtTokenProvider.getExpirationMillis()
        );

        LoginResponse response = new LoginResponse(
                authResult.getId(),
                authResult.getUsername(),
                authResult.getEmail(),
                authResult.getRole()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(response);
    }

    /**
     * Dành cho Swagger UI, Postman và ứng dụng mobile.
     * JWT được trả trong JSON để client tự gửi Authorization: Bearer <token>.
     */
    @PostMapping("/login/token")
    @Operation(
            summary = "Token login",
            description = "Authenticate user and return JWT for Bearer authentication"
    )
    public ResponseEntity<JwtResponse> loginForToken(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        JwtResponse response = authApplicationService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Tạo/lấy CSRF token cho Browser.
     * Spring tự lưu giá trị trong cookie XSRF-TOKEN.
     */
    @GetMapping("/csrf")
    @Operation(
            summary = "Get CSRF token",
            description = "Initialize the XSRF-TOKEN cookie for browser requests"
    )
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        ));
    }

    /**
     * Dành cho Browser: xóa access_token HttpOnly cookie.
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = "Clear JWT HttpOnly cookie"
    )
    public ResponseEntity<Map<String, Object>> logout() {
        ResponseCookie cleanCookie = jwtCookieService.getCleanJwtCookie();

        SecurityContextHolder.clearContext();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.OK.value());
        body.put("message", "Logged out successfully");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(body);
    }

    @PostMapping("/register")
    @Operation(
            summary = "User Registration",
            description = "Register a new user with default USER role."
    )
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authApplicationService.register(registerRequest));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Forgot Password",
            description = "Send OTP to user's email for password reset"
    )
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authApplicationService.sendPasswordResetOtp(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.OK.value());
        body.put("message", "OTP has been sent to your email. It expires in 5 minutes.");

        return ResponseEntity.ok(body);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset Password",
            description = "Reset password using OTP sent to email"
    )
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authApplicationService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}