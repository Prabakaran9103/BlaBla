package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.RequestArchive;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestArchiveRepository extends JpaRepository<RequestArchive, String> {
    Optional<RequestArchive> findByUserIdAndRequestId(String userId, String requestId);
    List<RequestArchive> findByUserIdAndArchivedTrue(String userId);
}
