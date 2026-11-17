package edu.iitm.cse.nodues.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSettings {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Column(nullable = false)
    private int unreadNotificationCount;

    @Column(nullable = false)
    private int emailReminderCounter;

    protected UserNotificationSettings() {
    }

    public UserNotificationSettings(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public int getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public int getEmailReminderCounter() {
        return emailReminderCounter;
    }

    public void changeEmailNotifications(boolean enabled) {
        emailNotificationsEnabled = enabled;
    }

    public void recordUnreadNotification() {
        unreadNotificationCount++;
        emailReminderCounter++;
    }

    public void markNotificationRead() {
        if (unreadNotificationCount > 0) {
            unreadNotificationCount--;
        }
    }

    public void resetEmailReminderCounter() {
        emailReminderCounter = 0;
    }
}
