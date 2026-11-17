package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.FacultyAssignment;
import edu.iitm.cse.nodues.domain.FacultyDecision;
import edu.iitm.cse.nodues.domain.HistoryActionType;
import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.RequestHistoryEntry;
import edu.iitm.cse.nodues.domain.RequestVersion;
import edu.iitm.cse.nodues.domain.RequestStatus;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.domain.NotificationType;
import edu.iitm.cse.nodues.repository.FacultyAssignmentRepository;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.RequestHistoryRepository;
import edu.iitm.cse.nodues.repository.RequestVersionRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestWorkflowService {

    private final NoDuesRequestRepository requestRepository;
    private final RequestVersionRepository versionRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final RequestHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RequestWorkflowService(
            NoDuesRequestRepository requestRepository,
            RequestVersionRepository versionRepository,
            FacultyAssignmentRepository assignmentRepository,
            RequestHistoryRepository historyRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.versionRepository = versionRepository;
        this.assignmentRepository = assignmentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public NoDuesRequest createRequest(
            String studentId,
            String facultyAdvisorId,
            String details) {
        requireUser(studentId, UserRole.STUDENT);
        requireUser(facultyAdvisorId, UserRole.FACULTY);
        requireDetails(details);

        NoDuesRequest request = requestRepository.save(
                new NoDuesRequest(studentId, facultyAdvisorId));
        versionRepository.save(new RequestVersion(
                request.getId(), 1, details.trim(), studentId));
        addHistory(request, studentId, HistoryActionType.CREATED, null);
        userRepository.findAll().stream()
                .filter(user -> user.isActive() && user.getRole() == UserRole.STAFF)
                .forEach(user -> notificationService.create(user.getId(), request.getId(),
                        NotificationType.REQUEST_UPDATED, "A new No Dues request is waiting for Staff review."));
        return request;
    }

    @Transactional
    public NoDuesRequest forwardRequest(
            String requestId,
            String staffId,
            List<String> facultyIds) {
        requireUser(staffId, UserRole.STAFF);
        if (facultyIds == null || facultyIds.size() > 5) {
            throw new IllegalArgumentException("A request can have zero to five Faculty members");
        }

        NoDuesRequest request = getRequest(requestId);
        ensureStaffCanManage(request, staffId);
        Set<String> uniqueFacultyIds = new HashSet<>(facultyIds);
        if (uniqueFacultyIds.size() != facultyIds.size()) {
            throw new IllegalArgumentException("A Faculty member cannot be selected twice");
        }
        for (String facultyId : facultyIds) {
            requireUser(facultyId, UserRole.FACULTY);
        }

        request.forwardTo(staffId);
        requestRepository.save(request);
        for (int position = 0; position < facultyIds.size(); position++) {
            assignmentRepository.save(
                    new FacultyAssignment(requestId, facultyIds.get(position), position));
        }
        addHistory(request, staffId, HistoryActionType.FORWARDED, null);
        facultyIds.forEach(facultyId -> notificationService.create(facultyId, requestId,
                NotificationType.REQUEST_UPDATED, "A No Dues request was assigned to you."));
        if (facultyIds.isEmpty()) {
            request.readyForClosure();
            requestRepository.save(request);
            if (request.getAssignedStaffId() != null) {
                notificationService.create(request.getAssignedStaffId(), request.getId(),
                        NotificationType.READY_FOR_CLOSURE,
                        "No Faculty members were selected. This request is ready for closure.");
            }
        }
        return request;
    }

    @Transactional
    public NoDuesRequest approveRequest(
            String requestId,
            String facultyId,
            String remark) {
        User faculty = requireUser(facultyId, UserRole.FACULTY);
        NoDuesRequest request = getRequest(requestId);
        FacultyAssignment assignment = getAssignment(requestId, faculty.getId());
        ensureFacultyCanAct(request);

        assignment.decide(FacultyDecision.APPROVED);
        assignmentRepository.save(assignment);
        addHistory(request, facultyId, HistoryActionType.FACULTY_APPROVED, normalizeRemark(remark));
        notificationService.create(request.getStudentId(), requestId,
                NotificationType.REQUEST_UPDATED, "A Faculty member approved your No Dues request.");
        markReadyForClosureIfFullyApproved(request);
        return request;
    }

    @Transactional
    public NoDuesRequest rejectRequest(
            String requestId,
            String facultyId,
            String reason) {
        User faculty = requireUser(facultyId, UserRole.FACULTY);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        NoDuesRequest request = getRequest(requestId);
        FacultyAssignment assignment = getAssignment(requestId, faculty.getId());
        ensureFacultyCanAct(request);

        assignment.decide(FacultyDecision.REJECTED);
        assignmentRepository.save(assignment);
        request.requestRevision();
        requestRepository.save(request);
        addHistory(request, facultyId, HistoryActionType.FACULTY_REJECTED, reason.trim());
        notificationService.create(request.getStudentId(), requestId,
                NotificationType.REQUEST_UPDATED, "A Faculty member requested a correction: " + reason.trim());
        return request;
    }

    @Transactional
    public NoDuesRequest withdrawDecision(String requestId, String facultyId) {
        User faculty = requireUser(facultyId, UserRole.FACULTY);
        NoDuesRequest request = getRequest(requestId);
        FacultyAssignment assignment = getAssignment(requestId, faculty.getId());
        ensureFacultyCanAct(request);

        assignment.withdraw();
        assignmentRepository.save(assignment);
        addHistory(request, facultyId, HistoryActionType.DECISION_WITHDRAWN, null);
        notificationService.create(request.getStudentId(), requestId,
                NotificationType.REQUEST_UPDATED,
                "A Faculty member withdrew a decision on your No Dues request.");
        return request;
    }

    @Transactional
    public NoDuesRequest resubmitRequest(
            String requestId,
            String studentId,
            String details) {
        requireDetails(details);
        NoDuesRequest request = getRequest(requestId);
        if (!request.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Only the request student can resubmit it");
        }
        if (request.getStatus() != RequestStatus.REVISION_REQUESTED) {
            throw new IllegalStateException("Only rejected requests can be resubmitted");
        }

        request.resubmit();
        requestRepository.save(request);
        versionRepository.save(new RequestVersion(
                requestId, request.getCurrentVersionNumber(), details.trim(), studentId));
        addHistory(request, studentId, HistoryActionType.RESUBMITTED, null);
        userRepository.findAll().stream()
                .filter(user -> user.isActive() && user.getRole() == UserRole.STAFF)
                .forEach(user -> notificationService.create(user.getId(), requestId,
                        NotificationType.REQUEST_UPDATED, "A No Dues request was resubmitted."));
        return request;
    }

    @Transactional
    public NoDuesRequest closeRequest(String requestId, String staffId) {
        requireUser(staffId, UserRole.STAFF);
        NoDuesRequest request = getRequest(requestId);
        if (!staffId.equals(request.getAssignedStaffId())) {
            throw new IllegalStateException("Only the assigned Staff member can close this request");
        }
        request.close();
        requestRepository.save(request);
        addHistory(request, staffId, HistoryActionType.CLOSED, null);
        notificationService.create(request.getStudentId(), requestId,
                NotificationType.REQUEST_UPDATED, "Your No Dues request was closed.");
        return request;
    }

    private void markReadyForClosureIfFullyApproved(NoDuesRequest request) {
        List<FacultyAssignment> assignments =
                assignmentRepository.findByRequestIdOrderByDisplayPositionAsc(request.getId());
        if (!assignments.isEmpty()
                && assignments.stream()
                        .allMatch(assignment -> assignment.getCurrentDecision() == FacultyDecision.APPROVED)) {
            request.readyForClosure();
            requestRepository.save(request);
            if (request.getAssignedStaffId() != null) {
                notificationService.create(request.getAssignedStaffId(), request.getId(),
                        NotificationType.READY_FOR_CLOSURE,
                        "All Faculty members approved this request. It is ready for closure.");
            }
            notificationService.createWithEmail(request.getStudentId(), request.getId(),
                    NotificationType.READY_FOR_CLOSURE,
                    "All Faculty members approved your No Dues request.");
        }
    }

    private void ensureFacultyCanAct(NoDuesRequest request) {
        if (request.getStatus() != RequestStatus.IN_FACULTY_REVIEW
                && request.getStatus() != RequestStatus.REVISION_REQUESTED) {
            throw new IllegalStateException("Faculty decisions are not open for this request");
        }
    }

    private void ensureStaffCanManage(NoDuesRequest request, String staffId) {
        if (request.getStatus() != RequestStatus.WAITING_FOR_STAFF) {
            throw new IllegalStateException("This request is no longer waiting for Staff");
        }
        if (request.getAssignedStaffId() != null
                && !request.getAssignedStaffId().equals(staffId)) {
            throw new IllegalStateException("Only the assigned Staff member can manage this request");
        }
    }

    private FacultyAssignment getAssignment(String requestId, String facultyId) {
        return assignmentRepository.findByRequestIdAndFacultyUserId(requestId, facultyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "This Faculty member is not assigned to the request"));
    }

    private NoDuesRequest getRequest(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request was not found"));
    }

    private User requireUser(String userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        if (!user.isActive() || user.getRole() != role) {
            throw new IllegalArgumentException("User does not have the required role");
        }
        return user;
    }

    private void addHistory(
            NoDuesRequest request,
            String actorUserId,
            HistoryActionType actionType,
            String remark) {
        historyRepository.save(new RequestHistoryEntry(
                request.getId(), actorUserId, actionType, remark));
    }

    private static void requireDetails(String details) {
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Request details are required");
        }
    }

    private static String normalizeRemark(String remark) {
        return remark == null || remark.isBlank() ? null : remark.trim();
    }
}
