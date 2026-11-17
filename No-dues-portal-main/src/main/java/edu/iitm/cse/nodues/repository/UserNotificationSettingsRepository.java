package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.UserNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingsRepository
        extends JpaRepository<UserNotificationSettings, String> {
}
