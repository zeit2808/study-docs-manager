package com.studydocs.manager.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class GmailValidator implements ConstraintValidator<ValidGmail,String> {
    private static final String GMAIL_PATTERN =
            "^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?@gmail\\.com$";
    @Override
    public void initialize(ValidGmail constraintAnnotation) {}
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context){
        if (email == null || email.trim().isEmpty() ){
            return true; // Cho phép null/empty, validation khác sẽ xử lý
        }
        String lowerEmail = email.toLowerCase().trim();

        // Nếu là @gmail.com thì validate nghiêm ngặt
        if (lowerEmail.endsWith("@gmail.com")){
            return lowerEmail.matches(GMAIL_PATTERN);
        }
        // BẮT BUỘC chỉ cho phép @gmail.com
        return false;
    }
}
