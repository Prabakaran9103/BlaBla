package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.SystemSettings;
import edu.iitm.cse.nodues.domain.UserNotificationSettings;
import edu.iitm.cse.nodues.repository.SystemSettingsRepository;
import edu.iitm.cse.nodues.repository.UserNotificationSettingsRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationReminderService {

    private final UserNotificationSettingsRepository settingsRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final PortalEmailService emailService;

    public NotificationReminderService(
            UserNotificationSettingsRepository settingsRepository,
            SystemSettingsRepository systemSettingsRepository,
            PortalEmailService emailService) {
        this.settingsRepository = settingsRepository;
        this.systemSettingsRepository = systemSettingsRepository;
        this.emailService = emailService;
    }

    @Transactional
    public List<String> usersReachingReminderThreshold() {
        int threshold = systemSettingsRepository.findById(1L)
                .orElseGet(SystemSettings::new)
                .getUnreadReminderThreshold();
        return settingsRepository.findAll().stream()
                .filter(settings -> settings.getEmailReminderCounter() >= threshold)
                .map(UserNotificationSettings::getUserId)
                .toList();
    }

    @Transactional
    public void resetReminderCounter(String userId) {
        UserNotificationSettings settings = settingsRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification settings were not found"));
        settings.resetEmailReminderCounter();
        settingsRepository.save(settings);
    }

    @Transactional
    public void sendDueReminders() {
        for (String userId : usersReachingReminderThreshold()) {
            emailService.sendUnreadReminder(userId);
            resetReminderCounter(userId);
        }
    }
}
