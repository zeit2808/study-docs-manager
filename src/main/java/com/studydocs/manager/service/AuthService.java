package com.studydocs.manager.service;

import com.studydocs.manager.dto.JwtResponse;
import com.studydocs.manager.dto.LoginRequest;
import com.studydocs.manager.dto.RegisterRequest;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
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

    @Transactional
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest
                        .getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Set<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .collect(Collectors.toSet());
        return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles);
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
        Set<Role> roles = new HashSet<>();
        if (registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));
            roles.add(userRole);
        } else {
            registerRequest.getRoles().forEach(roleName -> {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new RuntimeException("Role " + roleName + " not found"));
                roles.add(role);
            });
        }
        user.setRoles(roles);
        return userRepository.save(user);
    }
}
