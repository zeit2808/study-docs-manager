package com.studydocs.manager.dto;

import com.studydocs.manager.validation.ValidGmail;
import com.studydocs.manager.validation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileUpdateRequest {
    @Email(message = "Email should be valid")
    @ValidGmail(message = "Email @gmail.com Invalid. Format : username@gmail.com")
    private String email;
    
    private String fullname;

    @ValidPhoneNumber(
            message = "Phonenumber Invalid. Please format  (Such as: +84123456789, 0123456789)"
    )
    private String phone;

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
}
