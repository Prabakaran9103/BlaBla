package edu.iitm.cse.nodues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.iitm.cse.nodues.domain.FacultyAssignment;
import edu.iitm.cse.nodues.domain.FacultyDecision;
import edu.iitm.cse.nodues.domain.LoginMethod;
import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.RequestStatus;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.FacultyAssignmentRepository;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.RequestHistoryRepository;
import edu.iitm.cse.nodues.repository.RequestVersionRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import edu.iitm.cse.nodues.service.RequestWorkflowService;
import edu.iitm.cse.nodues.service.RequestManagementService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RequestWorkflowServiceTests {

    @Autowired
    private RequestWorkflowService service;

    @Autowired
    private RequestManagementService managementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoDuesRequestRepository requestRepository;

    @Autowired
    private RequestVersionRepository versionRepository;

    @Autowired
    private FacultyAssignmentRepository assignmentRepository;

    @Autowired
    private RequestHistoryRepository historyRepository;

    private User student;
    private User advisor;
    private User faculty;
    private User staff;
    private User otherStaff;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        assignmentRepository.deleteAll();
        versionRepository.deleteAll();
        requestRepository.deleteAll();
        userRepository.deleteAll();

        student = userRepository.save(new User(
                "Student", "student@smail.iitm.ac.in", UserRole.STUDENT, LoginMethod.PASSWORD));
        advisor = userRepository.save(new User(
                "Advisor", "advisor@cse.iitm.ac.in", UserRole.FACULTY, LoginMethod.PASSWORD));
        faculty = userRepository.save(new User(
                "Faculty", "faculty0@cse.iitm.ac.in", UserRole.FACULTY, LoginMethod.PASSWORD));
        staff = userRepository.save(new User(
                "Staff", "staff0@cse.iitm.ac.in", UserRole.STAFF, LoginMethod.PASSWORD));
        otherStaff = userRepository.save(new User(
                "Other Staff", "other.staff@cse.iitm.ac.in", UserRole.STAFF, LoginMethod.PASSWORD));
    }

    @Test
    void rejectionAndResubmissionKeepTheSameRequestAndEarlierVersion() {
        NoDuesRequest request = service.createRequest(
                student.getId(), advisor.getId(), "Initial details");
        service.forwardRequest(request.getId(), staff.getId(), List.of(faculty.getId()));

        service.rejectRequest(request.getId(), faculty.getId(), "Please add the missing detail");
        NoDuesRequest rejected = requestRepository.findById(request.getId()).orElseThrow();
        assertEquals(RequestStatus.REVISION_REQUESTED, rejected.getStatus());

        service.resubmitRequest(request.getId(), student.getId(), "Corrected details");
        NoDuesRequest resubmitted = requestRepository.findById(request.getId()).orElseThrow();
        assertEquals(request.getId(), resubmitted.getId());
        assertEquals(2, resubmitted.getCurrentVersionNumber());
        assertEquals(2, versionRepository.findByRequestIdOrderByVersionNumberAsc(request.getId()).size());
    }

    @Test
    void everyFacultyApprovalMakesRequestReadyForStaffClosure() {
        NoDuesRequest request = service.createRequest(
                student.getId(), advisor.getId(), "Details");
        service.forwardRequest(request.getId(), staff.getId(), List.of(advisor.getId(), faculty.getId()));

        service.approveRequest(request.getId(), advisor.getId(), null);
        assertEquals(RequestStatus.IN_FACULTY_REVIEW,
                requestRepository.findById(request.getId()).orElseThrow().getStatus());

        service.approveRequest(request.getId(), faculty.getId(), "Approved");
        assertEquals(RequestStatus.AWAITING_STAFF_CLOSURE,
                requestRepository.findById(request.getId()).orElseThrow().getStatus());
        assertEquals(FacultyDecision.APPROVED,
                assignmentRepository.findByRequestIdAndFacultyUserId(request.getId(), faculty.getId())
                        .orElseThrow().getCurrentDecision());
    }

    @Test
    void zeroFacultyRequestCanBeClosedDirectly() {
        NoDuesRequest request = service.createRequest(
                student.getId(), advisor.getId(), "No dues details");
        service.forwardRequest(request.getId(), staff.getId(), List.of());
        assertEquals(RequestStatus.AWAITING_STAFF_CLOSURE,
                requestRepository.findById(request.getId()).orElseThrow().getStatus());

        service.closeRequest(request.getId(), staff.getId());
        assertEquals(RequestStatus.CLOSED,
                requestRepository.findById(request.getId()).orElseThrow().getStatus());
    }

    @Test
    void rejectionWithoutReasonIsRejected() {
        NoDuesRequest request = service.createRequest(
                student.getId(), advisor.getId(), "Details");
        service.forwardRequest(request.getId(), staff.getId(), List.of(faculty.getId()));

        assertThrows(IllegalArgumentException.class,
                () -> service.rejectRequest(request.getId(), faculty.getId(), " "));
    }

    @Test
    void onlyTheOwningStaffMemberCanDeleteAForwardedRequest() {
        NoDuesRequest request = service.createRequest(
                student.getId(), advisor.getId(), "Details");
        service.forwardRequest(request.getId(), staff.getId(), List.of(faculty.getId()));

        assertThrows(IllegalStateException.class,
                () -> managementService.deleteByStaff(otherStaff.getId(), request.getId()));

        managementService.deleteByStaff(staff.getId(), request.getId());
        assertEquals(RequestStatus.DELETED,
                requestRepository.findById(request.getId()).orElseThrow().getStatus());
    }
}
