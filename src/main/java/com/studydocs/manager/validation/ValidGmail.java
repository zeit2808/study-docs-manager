package com.studydocs.manager.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = GmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGmail {
    String message() default "Email @gmail.com Invalid. Format: username@gmail.com";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}