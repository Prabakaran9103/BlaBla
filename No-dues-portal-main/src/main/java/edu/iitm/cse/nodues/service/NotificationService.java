package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.Notification;
import edu.iitm.cse.nodues.domain.NotificationType;
import edu.iitm.cse.nodues.domain.UserNotificationSettings;
import edu.iitm.cse.nodues.repository.NotificationRepository;
import edu.iitm.cse.nodues.repository.UserNotificationSettingsRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingsRepository settingsRepository;
    private final PortalEmailService emailService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserNotificationSettingsRepository settingsRepository,
            PortalEmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.settingsRepository = settingsRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Notification create(
            String userId,
            String requestId,
            NotificationType type,
            String message) {
        UserNotificationSettings settings = getOrCreateSettings(userId);
        settings.recordUnreadNotification();
        settingsRepository.save(settings);
        return notificationRepository.save(new Notification(userId, requestId, type, message));
    }

    @Transactional
    public Notification createWithEmail(
            String userId,
            String requestId,
            NotificationType type,
            String message) {
        Notification notification = create(userId, requestId, type, message);
        emailService.sendNormalEvent(userId, "No Dues Portal notification", message);
        return notification;
    }

    @Transactional
    public void markRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification was not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("This notification belongs to another user");
        }
        if (!notification.isRead()) {
            notification.markRead();
            notificationRepository.save(notification);
            UserNotificationSettings settings = getOrCreateSettings(userId);
            settings.markNotificationRead();
            settingsRepository.save(settings);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> listForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(notification -> !notification.isRead())
                .count();
    }

    @Transactional
    public UserNotificationSettings changeEmailPreference(String userId, boolean enabled) {
        UserNotificationSettings settings = getOrCreateSettings(userId);
        settings.changeEmailNotifications(enabled);
        return settingsRepository.save(settings);
    }

    private UserNotificationSettings getOrCreateSettings(String userId) {
        return settingsRepository.findById(userId)
                .orElseGet(() -> settingsRepository.save(new UserNotificationSettings(userId)));
    }
}
