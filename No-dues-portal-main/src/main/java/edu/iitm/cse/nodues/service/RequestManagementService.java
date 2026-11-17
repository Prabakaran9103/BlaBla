package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.RequestArchive;
import edu.iitm.cse.nodues.domain.RequestStatus;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.RequestArchiveRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class RequestManagementService {

    private static final Duration STAFF_RECOVERY_PERIOD = Duration.ofDays(3);

    private final NoDuesRequestRepository requestRepository;
    private final RequestArchiveRepository archiveRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RequestManagementService(
            NoDuesRequestRepository requestRepository,
            RequestArchiveRepository archiveRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.archiveRepository = archiveRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void archive(String userId, String requestId) {
        requireActiveUser(userId);
        getRequest(requestId);
        RequestArchive archive = archiveRepository.findByUserIdAndRequestId(userId, requestId)
                .orElseGet(() -> new RequestArchive(userId, requestId));
        archive.setArchived(true);
        archiveRepository.save(archive);
    }

    @Transactional
    public void unarchive(String userId, String requestId) {
        requireActiveUser(userId);
        RequestArchive archive = archiveRepository.findByUserIdAndRequestId(userId, requestId)
                .orElseThrow(() -> new IllegalArgumentException("Archive record was not found"));
        archive.setArchived(false);
        archiveRepository.save(archive);
    }

    @Transactional
    public void deleteByStaff(String staffId, String requestId) {
        requireRole(staffId, UserRole.STAFF);
        NoDuesRequest request = getRequest(requestId);
        if (!staffId.equals(request.getAssignedStaffId())) {
            throw new IllegalStateException("Only the Staff member who owns this request can delete it");
        }
        request.delete(staffId);
        requestRepository.save(request);
        notificationService.create(request.getStudentId(), requestId,
                edu.iitm.cse.nodues.domain.NotificationType.REQUEST_DELETED,
                "Your No Dues request was deleted by Staff.");
    }

    @Transactional
    public void restoreByDeletingStaff(String staffId, String requestId) {
        requireRole(staffId, UserRole.STAFF);
        NoDuesRequest request = getRequest(requestId);
        if (!staffId.equals(request.getDeletedByUserId())) {
            throw new IllegalArgumentException("Only the Staff member who deleted it can restore it");
        }
        if (request.getDeletedAt() == null
                || Duration.between(request.getDeletedAt(), Instant.now()).compareTo(STAFF_RECOVERY_PERIOD) > 0) {
            throw new IllegalStateException("The three-day recovery period has expired");
        }
        request.restore();
        requestRepository.save(request);
        notificationService.create(request.getStudentId(), requestId,
                edu.iitm.cse.nodues.domain.NotificationType.REQUEST_RESTORED,
                "Your No Dues request was restored.");
    }

    @Transactional
    public void deleteByAdmin(String adminId, String requestId) {
        requireRole(adminId, UserRole.ADMIN);
        NoDuesRequest request = getRequest(requestId);
        request.delete(adminId);
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public boolean isArchived(String userId, String requestId) {
        return archiveRepository.findByUserIdAndRequestId(userId, requestId)
                .map(RequestArchive::isArchived)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Set<String> archivedRequestIds(String userId) {
        return archiveRepository.findByUserIdAndArchivedTrue(userId).stream()
                .map(RequestArchive::getRequestId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<NoDuesRequest> archivedRequests(String userId, List<NoDuesRequest> requests) {
        Set<String> archivedIds = archivedRequestIds(userId);
        return requests.stream().filter(request -> archivedIds.contains(request.getId())).toList();
    }

    private NoDuesRequest getRequest(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request was not found"));
    }

    private void requireRole(String userId, UserRole role) {
        userRepository.findById(userId)
                .filter(user -> user.isActive() && user.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("User does not have the required role"));
    }

    private void requireActiveUser(String userId) {
        userRepository.findById(userId)
                .filter(user -> user.isActive())
                .orElseThrow(() -> new IllegalArgumentException("User is not active"));
    }
}
