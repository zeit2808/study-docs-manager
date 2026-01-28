package com.studydocs.manager.service;
import com.studydocs.manager.search.UserSearchService;
import com.studydocs.manager.dto.UserResponse;
import com.studydocs.manager.dto.UserUpdateRequest;
import com.studydocs.manager.entity.Role;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.RoleRepository;
import com.studydocs.manager.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired(required = false)
    private UserSearchService userSearchService;
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    @Cacheable(cacheNames = "usersById", key = "#id")
    public UserResponse getUserById(Long id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToResponse(user);
    }
    @Transactional
    @CacheEvict(cacheNames = {"usersById", "usersByUsername"}, allEntries = true)
    public UserResponse updateUser(Long id, UserUpdateRequest updateRequest){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().isEmpty()){
            if (!user.getEmail().equals(updateRequest.getEmail()) &&
            userRepository.existsByEmail(updateRequest.getEmail())){
                throw new RuntimeException("Email is already in use");
            }
            user.setEmail(updateRequest.getEmail());
        }
        if (updateRequest.getPassword() != null && !updateRequest
                .getPassword().isEmpty()){
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }
        if (updateRequest.getFullname() != null) {
            user.setFullname(updateRequest.getFullname());
        }

        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }

        if (updateRequest.getEnabled() != null) {
            user.setEnabled(updateRequest.getEnabled());
        }
        if (updateRequest.getRoles()!=null && !updateRequest.getRoles()
                .isEmpty()){
            Set<Role> roles = new HashSet<>();
            updateRequest.getRoles().forEach(roleName -> {
                 Role role = roleRepository.findByName(roleName.toUpperCase())
                         .orElseThrow(()->new RuntimeException("Role "+roleName+" not found"));
                    roles.add(role);
            });
            user.setRoles(roles);
        }
        User updatedUser = userRepository.save(user);
        if (userSearchService != null) {
            userSearchService.indexUser(updatedUser);
        }
        return convertToResponse(updatedUser);
    }
    @Transactional
    @CacheEvict(cacheNames = {"usersById", "usersByUsername"}, allEntries = true)
    public void deleteUser(Long id){
        if (!userRepository.existsById(id)){
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
        if (userSearchService != null) {
            userSearchService.deleteFromIndex(id);
        }
    }
    @Transactional
    @Cacheable(cacheNames = "usersByUsername", key = "#username")
    public UserResponse getUserByUsername(String username){
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToResponse(user);
    }
    private UserResponse convertToResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsename(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullname(user.getFullname());
        response.setPhone(user.getPhone());
        response.setEnabled(user.getEnabled());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdateAt(user.getUpdateAt());
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        response.setRoles(roles);
        return response;
    }

}
