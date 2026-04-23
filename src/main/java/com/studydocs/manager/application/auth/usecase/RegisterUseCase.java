package com.studydocs.manager.application.auth.usecase;

import com.studydocs.manager.dto.auth.RegisterRequest;
import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.ConflictException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.service.user.UserResponseMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;

    public RegisterUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional
    public UserResponse execute(RegisterRequest request) {
        validateUniqueness(request.getUsername(), request.getEmail(), request.getPhone());

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Role USER not found", "ROLE_NOT_FOUND", "role"));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullname(request.getFullname());
        user.setPhone(request.getPhone());
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);

        return userResponseMapper.toResponse(userRepository.save(user));
    }

    private void validateUniqueness(String username, String email, String phone) {
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
