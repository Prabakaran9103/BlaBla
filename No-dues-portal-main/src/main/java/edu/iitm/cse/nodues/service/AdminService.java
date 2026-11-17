package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.SystemSettings;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.domain.LoginMethod;
import edu.iitm.cse.nodues.repository.SystemSettingsRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final SystemSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            SystemSettingsRepository settingsRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> users(String adminId) {
        requireAdmin(adminId);
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemSettings settings(String adminId) {
        requireAdmin(adminId);
        return settingsRepository.findById(1L).orElseGet(SystemSettings::new);
    }

    @Transactional(readOnly = true)
    public String adminIdForEmail(String email) {
        User admin = userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.isActive() && user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Admin permission is required"));
        return admin.getId();
    }

    @Transactional
    public void changeReminderThreshold(String adminId, int threshold) {
        requireAdmin(adminId);
        SystemSettings settings = settingsRepository.findById(1L).orElseGet(SystemSettings::new);
        settings.changeUnreadReminderThreshold(threshold);
        settingsRepository.save(settings);
    }

    @Transactional
    public User addUser(String adminId, String name, String email, UserRole role, String password) {
        requireAdmin(adminId);
        if (role != UserRole.STAFF && role != UserRole.FACULTY) {
            throw new IllegalArgumentException("Only Staff or Faculty users can be added");
        }

        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account already exists for this email");
        }
        User user = new User(name.trim(), normalizedEmail, role, LoginMethod.GOOGLE);
        if (password != null && !password.isBlank()) {
            if (password.length() < 8) {
                throw new IllegalArgumentException("Password must have at least eight characters");
            }
            user.setPasswordHash(passwordEncoder.encode(password));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void removeUser(String adminId, String userId) {
        requireAdmin(adminId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("The Admin account cannot be removed");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void updateUser(String adminId, String userId, String name, String email, UserRole role) {
        requireAdmin(adminId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (user.getRole() == UserRole.ADMIN || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("The Admin account cannot be edited here");
        }
        if (name == null || name.isBlank() || email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Name and a valid email are required");
        }
        userRepository.findByEmailIgnoreCase(email.trim())
                .filter(found -> !found.getId().equals(userId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("This email is already in use");
                });
        user.changeName(name.trim());
        user.changeEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
        if (role != null) {
            user.changeRole(role);
        }
        userRepository.save(user);
    }

    @Transactional
    public void changeUserRole(String adminId, String userId, UserRole role) {
        requireAdmin(adminId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (user.getRole() == UserRole.ADMIN || role == UserRole.ADMIN) {
            throw new IllegalArgumentException("The Admin role cannot be changed here");
        }
        user.changeRole(role);
        userRepository.save(user);
    }

    private void requireAdmin(String adminId) {
        userRepository.findById(adminId)
                .filter(user -> user.isActive() && user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException("Admin permission is required"));
    }
}
