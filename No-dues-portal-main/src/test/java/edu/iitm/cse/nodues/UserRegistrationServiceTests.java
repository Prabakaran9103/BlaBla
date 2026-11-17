package edu.iitm.cse.nodues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.iitm.cse.nodues.domain.User;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.UserRepository;
import edu.iitm.cse.nodues.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserRegistrationServiceTests {

    @Autowired
    private UserRegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void registrationAllowsStudentsAndHashesPassword() {
        User user = registrationService.register(
                "Student", "STUDENT@SMAIL.IITM.AC.IN", "password123");

        assertEquals(UserRole.STUDENT, user.getRole());
        assertEquals("student@smail.iitm.ac.in", user.getEmail());
        assertNotEquals("password123", user.getPasswordHash());
    }

    @Test
    void nonStudentDomainIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> registrationService.register(
                "Student", "student@gmail.com", "password123"));
    }
}
