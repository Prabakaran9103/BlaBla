package edu.iitm.cse.nodues.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

    @Id
    private long id = 1L;

    @Column(nullable = false)
    private int unreadReminderThreshold = 10;

    public SystemSettings() {
    }

    public int getUnreadReminderThreshold() {
        return unreadReminderThreshold;
    }

    public void changeUnreadReminderThreshold(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("Reminder threshold must be at least one");
        }
        unreadReminderThreshold = threshold;
    }
}
