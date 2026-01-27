package com.studydocs.manager.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber,String> {
    // Pattern quốc tế: Phải bắt đầu bằng + hoặc 00, sau đó là mã quốc gia và số điện thoại
    private static final String INTERNATIONAL_PHONE_PATTERN =
            "^(\\+[1-9]\\d{1,2}|00[1-9]\\d{1,2})[1-9]\\d{6,14}$";
    
    // Pattern Việt Nam cụ thể: +84, 0084, 84, hoặc 0 + đầu số hợp lệ
    private static final String VIETNAM_PHONE_PATTERN =
            "^(\\+84|0084|84|0)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-6|8|9]|9[0-4|6-9])[0-9]{7}$";
    
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context){
        if (phone == null || phone.trim().isEmpty()){
            return true; // Cho phép null/empty, validation khác sẽ xử lý
        }
        
        String cleanedPhone = phone.replaceAll("[\\s\\-()]", "");
        
        // Kiểm tra pattern Việt Nam trước (ưu tiên)
        if (cleanedPhone.matches(VIETNAM_PHONE_PATTERN)){
            return true;
        }
        
        // Kiểm tra pattern quốc tế (phải có + hoặc 00 ở đầu)
        if (cleanedPhone.matches(INTERNATIONAL_PHONE_PATTERN)){
            return true;
        }
        
        // Nếu không match bất kỳ pattern nào, trả về false
        return false;
    }
}
