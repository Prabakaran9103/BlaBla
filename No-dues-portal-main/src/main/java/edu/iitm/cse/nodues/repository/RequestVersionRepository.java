package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.RequestVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestVersionRepository extends JpaRepository<RequestVersion, String> {
    List<RequestVersion> findByRequestIdOrderByVersionNumberAsc(String requestId);
}
