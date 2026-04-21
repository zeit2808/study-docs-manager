package com.studydocs.manager.dto.auth;
import com.studydocs.manager.enums.RoleName;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.studydocs.manager.validation.ValidGmail;
import com.studydocs.manager.validation.ValidPhoneNumber;
import jakarta.validation.constraints.*;

public class AdminRegisterRequest {
    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email should be valid")
    @ValidGmail(message = "Email @gmail.com Invalid. Must Format : username@gmail.com")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.,;:_+\\-=^#()\\[\\]{}|<>]).{8,64}$", message = "Password must have upper, lower, digit, special char, no spaces")
    private String password;

    private String fullname;

    @JsonProperty("phone")
    @ValidPhoneNumber(message = "Invalid phone number format")
    private String phone;

    /**
     * Role được gán cho tài khoản mới.
     * Admin có thể chọn bất kỳ value trong {@link RoleName}.
     * Mặc định (nếu null): USER.
     */
    private RoleName role = RoleName.USER;

    private Boolean enabled;

    public AdminRegisterRequest() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role != null ? role : RoleName.USER;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
