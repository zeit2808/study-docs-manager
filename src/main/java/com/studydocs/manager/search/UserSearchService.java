package com.studydocs.manager.search;

import com.studydocs.manager.dto.UserResponse;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class UserSearchService {

    @Autowired(required = false)
    private UserSearchRepository userSearchRepository;

    @Autowired
    private UserRepository userRepository;

    public void indexUser(User user){
        if (userSearchRepository == null || user == null || user.getId() == null){
            return;
        }
        try {
            // Đúng thứ tự: id, username, email, fullname
            UserSearchDocument doc = new UserSearchDocument(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullname()
            );
            userSearchRepository.save(doc);
        } catch (Exception e) {
            // Log error nhưng không throw để app vẫn chạy được
            System.err.println("Failed to index user to Elasticsearch: " + e.getMessage());
        }
    }

    public void deleteFromIndex(Long userId){
        if (userSearchRepository == null) {
            return;
        }
        try {
            userSearchRepository.deleteById(userId);
        } catch (Exception e) {
            System.err.println("Failed to delete user from Elasticsearch: " + e.getMessage());
        }
    }

    public List<UserResponse> searchUsers(String keyword){
        if (userSearchRepository == null) {
            // Fallback: search từ MySQL nếu ES không available
            return searchUsersFromMySQL(keyword);
        }
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()){
            return List.of();
        }
        try {
            List<UserSearchDocument> docs =
                    userSearchRepository
                            .findByUsernameContainingIgnoreCaseOrFullnameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                                    kw, kw, kw);
            List<Long> ids = docs.stream()
                    .map(UserSearchDocument::getId)
                    .toList();
            List<User> users = userRepository.findAllById(ids);
            return users.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Fallback to MySQL search
            System.err.println("Elasticsearch search failed, falling back to MySQL: " + e.getMessage());
            return searchUsersFromMySQL(keyword);
        }
    }

    private List<UserResponse> searchUsersFromMySQL(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return List.of();
        }
        // Fallback search từ MySQL
        List<User> users = userRepository.findAll().stream()
                .filter(user -> 
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(kw.toLowerCase())) ||
                    (user.getFullname() != null && user.getFullname().toLowerCase().contains(kw.toLowerCase())) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(kw.toLowerCase()))
                )
                .toList();
        return users.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
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
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        response.setRoles(roles);
        return response;
    }
}
