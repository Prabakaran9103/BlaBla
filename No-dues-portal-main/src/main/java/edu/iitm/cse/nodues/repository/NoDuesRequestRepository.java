package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.RequestStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoDuesRequestRepository extends JpaRepository<NoDuesRequest, String> {
    List<NoDuesRequest> findByStudentIdOrderByUpdatedAtDesc(String studentId);

    List<NoDuesRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);
}
