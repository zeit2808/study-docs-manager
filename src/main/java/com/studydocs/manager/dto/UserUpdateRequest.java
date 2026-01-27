package com.studydocs.manager.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import com.studydocs.manager.validation.ValidGmail;
import com.studydocs.manager.validation.ValidPhoneNumber;
import java.util.Set;
public class UserUpdateRequest {

    @Email(message = "Email should be valid")
    @ValidGmail(message = "Email @gmail.com Invalid. Format : username@gmail.com")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    private String fullname;
    @ValidPhoneNumber(message = "Phonenumber Invalid. Please format  (Such as: +84123456789, 0123456789)")
    private String phone;
    private Boolean enabled;
    private Set<String> roles;

    public UserUpdateRequest() {
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
