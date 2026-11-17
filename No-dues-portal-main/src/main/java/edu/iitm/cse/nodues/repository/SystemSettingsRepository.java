package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
}
