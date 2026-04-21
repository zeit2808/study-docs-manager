package com.studydocs.manager.service.user;

import com.studydocs.manager.dto.user.UserResponse;
import com.studydocs.manager.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserResponseMapper {

    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullname(user.getFullname());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole() != null ? user.getRole().getName() : null);
        response.setAvatarObjectName(user.getAvatarObjectName());
        return response;
    }
}
