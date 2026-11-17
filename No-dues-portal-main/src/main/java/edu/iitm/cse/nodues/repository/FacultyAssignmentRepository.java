package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.FacultyAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyAssignmentRepository extends JpaRepository<FacultyAssignment, String> {
    List<FacultyAssignment> findByRequestIdOrderByDisplayPositionAsc(String requestId);

    Optional<FacultyAssignment> findByRequestIdAndFacultyUserId(String requestId, String facultyUserId);

    List<FacultyAssignment> findByFacultyUserId(String facultyUserId);
}
