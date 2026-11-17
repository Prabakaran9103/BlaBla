package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserNotificationSettings;
import edu.iitm.cse.nodues.repository.UserNotificationSettingsRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PortalEmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final UserNotificationSettingsRepository settingsRepository;
    private final boolean enabled;
    private final String from;
    private final String baseUrl;

    public PortalEmailService(
            JavaMailSender mailSender,
            UserRepository userRepository,
            UserNotificationSettingsRepository settingsRepository,
            @Value("${app.email.enabled:false}") boolean enabled,
            @Value("${app.email.from:no-reply@localhost}") String from,
            @Value("${app.portal.base-url:http://localhost:8080}") String baseUrl) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.enabled = enabled;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    public void sendNormalEvent(String userId, String subject, String text) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (!user.isActive()) {
            return;
        }
        UserNotificationSettings settings = settingsRepository.findById(userId).orElse(null);
        if (!enabled || (settings != null && !settings.isEmailNotificationsEnabled())) {
            return;
        }
        send(user.getEmail(), subject, text);
    }

    public void sendUnreadReminder(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (!user.isActive()) {
            return;
        }
        if (!enabled) {
            return;
        }
        send(user.getEmail(), "Unread No Dues notifications",
                "Please open the No Dues Portal and review your unread notifications.");
    }

    public void sendPasswordReset(String userId, String token) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (!user.isActive()) {
            throw new IllegalArgumentException("The account is not active");
        }
        if (!enabled) {
            throw new IllegalStateException("Email sending is not enabled");
        }
        String link = baseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        send(user.getEmail(), "Reset your No Dues Portal password",
                "Open this link within one hour to reset your password:\n\n" + link);
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
