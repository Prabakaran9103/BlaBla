package edu.iitm.cse.nodues.repository;

import edu.iitm.cse.nodues.domain.RequestFile;
import edu.iitm.cse.nodues.domain.RequestFileType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestFileRepository extends JpaRepository<RequestFile, String> {
    List<RequestFile> findByRequestId(String requestId);
    List<RequestFile> findByRequestIdAndVersionNumberAndFileType(
            String requestId, int versionNumber, RequestFileType fileType);
}
