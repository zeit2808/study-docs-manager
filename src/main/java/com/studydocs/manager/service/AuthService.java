package com.studydocs.manager.service;

import com.studydocs.manager.dto.JwtResponse;
import com.studydocs.manager.dto.LoginRequest;
import com.studydocs.manager.dto.RegisterRequest;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.studydocs.manager.search.UserSearchService;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired(required = false)
    private UserSearchService userSearchService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;
    
    @Autowired
    private LoginAttemptService loginAttemptService;


    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check locked trong Redis TRƯỚC khi authenticate
        java.time.LocalDateTime unlockTime = loginAttemptService.isAccountLocked(loginRequest.getUsername());
        if (unlockTime != null) {
            throw new RuntimeException("Account is locked until " + unlockTime);
        }

        try {
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Login success → reset counter trong Redis
            loginAttemptService.resetFailedAttempts(loginRequest.getUsername());

            String jwt = tokenProvider.generateToken(authentication);
            Set<String> roles = user.getRoles().stream()
                    .map(role -> "ROLE_" + role.getName())
                    .collect(Collectors.toSet());
            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles);

        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Login fail → tăng failed attempts trong Redis (không bị rollback)
            boolean isLocked = loginAttemptService.incrementFailedAttempts(loginRequest.getUsername());
            
            if (isLocked) {
                // Đã đạt ngưỡng và bị lock - log warning vì đây là security event
                unlockTime = loginAttemptService.isAccountLocked(loginRequest.getUsername());
                logger.warn("Account locked due to multiple failed login attempts - username: {}, unlockTime: {}", 
                           loginRequest.getUsername(), unlockTime);
                throw new RuntimeException("Account is locked until " + unlockTime + " due to 5 failed login attempts");
            }
            
            // Login sai nhưng chưa bị lock - không log (expected behavior)
            // Chỉ log nếu cần debug (có thể bật bằng log level)
            if (logger.isDebugEnabled()) {
                int attempts = loginAttemptService.getFailedAttempts(loginRequest.getUsername());
                logger.debug("Login failed - username: {}, current attempts: {}", 
                           loginRequest.getUsername(), attempts);
            }
            
            throw new RuntimeException("Invalid username or password");
        }
    }
    @Transactional
    public User register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setFullname(registerRequest.getFullname());
        user.setPhone(registerRequest.getPhone());
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        
        // Normal registration always sets USER role (ignore any roles in request)
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));
        roles.add(userRole);
        user.setRoles(roles);
        
        User saved = userRepository.save(user);
        // index vào Elasticsearch để hỗ trợ search nhanh (nếu ES available)
        if (userSearchService != null) {
            userSearchService.indexUser(saved);
        }
        return saved;
    }

    @Transactional
    public User registerByAdmin(com.studydocs.manager.dto.AdminRegisterRequest adminRegisterRequest) {
        if (userRepository.existsByUsername(adminRegisterRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(adminRegisterRequest.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }
        User user = new User();
        user.setUsername(adminRegisterRequest.getUsername());
        user.setEmail(adminRegisterRequest.getEmail());
        user.setFullname(adminRegisterRequest.getFullname());
        user.setPhone(adminRegisterRequest.getPhone());
        user.setEnabled(adminRegisterRequest.getEnabled() != null ? adminRegisterRequest.getEnabled() : true);
        user.setPassword(passwordEncoder.encode(adminRegisterRequest.getPassword()));
        
        // Admin can set any roles
        Set<Role> roles = new HashSet<>();
        if (adminRegisterRequest.getRoles() == null || adminRegisterRequest.getRoles().isEmpty()) {
            throw new RuntimeException("Roles must be provided when registering by admin");
        }
        adminRegisterRequest.getRoles().forEach(roleName -> {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Role " + roleName + " not found"));
            roles.add(role);
        });
        user.setRoles(roles);
        
        User saved = userRepository.save(user);
        // index vào Elasticsearch để hỗ trợ search nhanh (nếu ES available)
        if (userSearchService != null) {
            userSearchService.indexUser(saved);
        }
        return saved;
    }
}
