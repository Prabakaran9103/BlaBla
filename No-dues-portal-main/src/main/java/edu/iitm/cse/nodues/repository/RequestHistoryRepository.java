package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.RequestHistoryEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistoryEntry, String> {
    List<RequestHistoryEntry> findByRequestIdOrderByOccurredAtAsc(String requestId);
}
