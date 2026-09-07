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
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authApplicationService.sendPasswordResetOtp(request);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.OK.value());
        body.put(
                "message",
                "If an account exists for this email, a reset code will be sent."
        );

        return ResponseEntity.ok(body);
    }

    /**
     * Dành cho Browser / SPA:
     * Xác thực OTP, cập nhật mật khẩu mới và tự động đăng nhập (Auto-login).
     * JWT mới được nhét trực tiếp vào HttpOnly cookie, trả về thông tin user.
     */
    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset Password and Auto-Login (Browser)",
            description = "Reset password using OTP sent to email and automatically authenticate user via HttpOnly cookie"
    )
    public ResponseEntity<LoginResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        JwtResponse authResult = authApplicationService.resetPassword(request);

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
     * Dành cho Swagger UI, Postman và ứng dụng mobile:
     * Xác thực OTP, đổi mật khẩu và trả về JWT trong JSON để client dùng Authorization: Bearer <token>.
     */
    @PostMapping("/reset-password/token")
    @Operation(
            summary = "Reset Password and Return Token",
            description = "Reset password using OTP sent to email and return JWT for Bearer authentication"
    )
    public ResponseEntity<JwtResponse> resetPasswordForToken(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        JwtResponse response = authApplicationService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}