package edu.iitm.cse.nodues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.iitm.cse.nodues.domain.LoginMethod;
import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.Notification;
import edu.iitm.cse.nodues.domain.RequestFileType;
import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserNotificationSettings;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.FacultyAssignmentRepository;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.NotificationRepository;
import edu.iitm.cse.nodues.repository.RequestFileRepository;
import edu.iitm.cse.nodues.repository.RequestHistoryRepository;
import edu.iitm.cse.nodues.repository.RequestVersionRepository;
import edu.iitm.cse.nodues.repository.UserNotificationSettingsRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import edu.iitm.cse.nodues.service.NotificationService;
import edu.iitm.cse.nodues.service.RequestFileService;
import edu.iitm.cse.nodues.service.RequestWorkflowService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FileAndNotificationServiceTests {

    @Autowired
    private RequestFileService fileService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RequestWorkflowService workflowService;

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

    @Autowired
    private RequestFileRepository fileRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserNotificationSettingsRepository settingsRepository;

    private User student;
    private User advisor;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        settingsRepository.deleteAll();
        fileRepository.deleteAll();
        historyRepository.deleteAll();
        assignmentRepository.deleteAll();
        versionRepository.deleteAll();
        requestRepository.deleteAll();
        userRepository.deleteAll();

        student = userRepository.save(new User(
                "Student", "student@smail.iitm.ac.in", UserRole.STUDENT, LoginMethod.PASSWORD));
        advisor = userRepository.save(new User(
                "Advisor", "advisor@cse.iitm.ac.in", UserRole.FACULTY, LoginMethod.PASSWORD));
    }

    @Test
    void studentFilesAreLimitedAndOldFilesRemainAfterResubmission() {
        NoDuesRequest request = workflowService.createRequest(
                student.getId(), advisor.getId(), "Details");
        fileService.storeStudentFile(request.getId(), student.getId(), file("one.txt", "one"));
        fileService.storeStudentFile(request.getId(), student.getId(), file("two.txt", "two"));
        fileService.storeStudentFile(request.getId(), student.getId(), file("three.txt", "three"));
        assertThrows(IllegalArgumentException.class, () -> fileService.storeStudentFile(
                request.getId(), student.getId(), file("four.txt", "four")));
        assertEquals(3, fileRepository.findByRequestIdAndVersionNumberAndFileType(
                request.getId(), 1, RequestFileType.STUDENT_REQUEST).size());
    }

    @Test
    void notificationCountersAreIndependent() {
        Notification notification = notificationService.create(
                student.getId(), null, edu.iitm.cse.nodues.domain.NotificationType.REQUEST_UPDATED, "Updated");
        notificationService.create(
                student.getId(), null, edu.iitm.cse.nodues.domain.NotificationType.REQUEST_CLOSED, "Closed");
        UserNotificationSettings settings = settingsRepository.findById(student.getId()).orElseThrow();
        assertEquals(2, settings.getUnreadNotificationCount());
        assertEquals(2, settings.getEmailReminderCounter());

        notificationService.markRead(notification.getId(), student.getId());
        settings = settingsRepository.findById(student.getId()).orElseThrow();
        assertEquals(1, settings.getUnreadNotificationCount());
        assertEquals(2, settings.getEmailReminderCounter());
    }

    private static MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes());
    }
}
