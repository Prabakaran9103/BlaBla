package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.LoginMethod;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String name, String email, String password) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        String normalizedEmail = normalizeEmail(email);
        if (!normalizedEmail.endsWith("@smail.iitm.ac.in")) {
            throw new IllegalArgumentException(
                    "Only student emails ending with @smail.iitm.ac.in can register");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must have at least eight characters");
        }
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account already exists for this email");
        }
        User user = new User(
                name.trim(), normalizedEmail, UserRole.STUDENT, LoginMethod.PASSWORD);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid institute email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
