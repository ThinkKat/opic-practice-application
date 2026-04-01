package me.thinkcat.opic.practice.validation;

import me.thinkcat.opic.practice.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {

    public void validatePassword(String password) {
        String pattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(pattern)) {
            throw new ValidationException("Password must be at least 8 characters with uppercase, lowercase, digit, and special character");
        }
    }

    public void validateEmail(String email) {
        if (email == null || !email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            throw new ValidationException("Invalid email format");
        }
    }
}
