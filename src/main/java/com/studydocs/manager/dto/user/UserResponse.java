package com.studydocs.manager.dto.user;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullname;
    private String phone;
    private String role;
    private String avatarObjectName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarObjectName() {
        return avatarObjectName;
    }

    public void setAvatarObjectName(String avatarObjectName) {
        this.avatarObjectName = avatarObjectName;
    }
}
