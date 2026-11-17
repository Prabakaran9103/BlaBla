package edu.iitm.cse.nodues.config;

import edu.iitm.cse.nodues.domain.LoginMethod;
import edu.iitm.cse.nodues.domain.SystemSettings;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.SystemSettingsRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapConfiguration {

    @Bean
    CommandLineRunner seedPortalData(
            SystemSettingsRepository settingsRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        return args -> {
            if (!settingsRepository.existsById(1L)) {
                settingsRepository.save(new SystemSettings());
            }
            if (!adminEmail.isBlank() && !adminPassword.isBlank()
                    && userRepository.findAll().stream()
                            .noneMatch(user -> user.getRole() == UserRole.ADMIN)) {
                User admin = new User("Portal Admin", adminEmail, UserRole.ADMIN, LoginMethod.PASSWORD);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                userRepository.save(admin);
            }
        };
    }
}
