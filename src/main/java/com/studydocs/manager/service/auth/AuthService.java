package com.studydocs.manager.service.auth;
import java.time.LocalDateTime;
import com.studydocs.manager.dto.auth.AdminRegisterRequest;
import com.studydocs.manager.dto.auth.JwtResponse;
import com.studydocs.manager.dto.auth.LoginRequest;
import com.studydocs.manager.dto.auth.RegisterRequest;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.enums.RoleName;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.UnauthorizedException;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UnauthorizedException(
                        "Username/password is incorrect",
                        "INVALID_CREDENTIALS",
                        null));

        LocalDateTime unlockTime = loginAttemptService.isAccountLocked(loginRequest.getUsername());
        if (unlockTime != null) {
            throw new UnauthorizedException(
                    "Account is locked until " + unlockTime,
                    "ACCOUNT_LOCKED",
                    null);
        }
        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            loginAttemptService.resetFailedAttempts(loginRequest.getUsername());

            String jwt = tokenProvider.generateToken(authentication);
            String roleName = "ROLE_" + user.getRole().getName();
            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roleName);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            boolean isLocked = loginAttemptService.incrementFailedAttempts(loginRequest.getUsername());

            if (isLocked) {
                unlockTime = loginAttemptService.isAccountLocked(loginRequest.getUsername());
                logger.warn("Account locked due to multiple failed login attempts - username: {}, unlockTime: {}",
                        loginRequest.getUsername(), unlockTime);
                throw new UnauthorizedException(
                        "Account is locked until " + unlockTime + " due to 5 failed login attempts",
                        "ACCOUNT_LOCKED",
                        null);
            }

            if (logger.isDebugEnabled()) {
                int attempts = loginAttemptService.getFailedAttempts(loginRequest.getUsername());
                logger.debug("Login failed - username: {}, current attempts: {}",
                        loginRequest.getUsername(), attempts);
            }
            throw new UnauthorizedException(
                    "Username/password is incorrect",
                    "INVALID_CREDENTIALS",
                    null);
        }
    }

    @Transactional
    public User register(RegisterRequest registerRequest) {
        validateRegistrationUniqueness(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPhone());

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setFullname(registerRequest.getFullname());
        user.setPhone(registerRequest.getPhone());
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Role USER not found", "ROLE_NOT_FOUND", "role"));
        user.setRole(userRole);

        User saved = userRepository.save(user);
        return saved;
    }

    @Transactional
    public User registerByAdmin(AdminRegisterRequest req) {
        validateRegistrationUniqueness(req.getUsername(), req.getEmail(), req.getPhone());

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setFullname(req.getFullname());
        user.setPhone(req.getPhone());
        user.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        RoleName roleName = req.getRole() != null ? req.getRole() : RoleName.USER;
        Role role = roleRepository.findByName(roleName.name())
                .orElseThrow(() -> new NotFoundException(
                        "Role " + roleName + " not found in database",
                        "ROLE_NOT_FOUND",
                        "role"));
        user.setRole(role);

        User saved = userRepository.save(user);
        return saved;
    }

    private void validateRegistrationUniqueness(String username, String email, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken", "USERNAME_TAKEN", "username");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already in use", "EMAIL_TAKEN", "email");
        }
        if (phone != null && !phone.isBlank() && userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already in use", "PHONE_TAKEN", "phone");
        }
    }
}
