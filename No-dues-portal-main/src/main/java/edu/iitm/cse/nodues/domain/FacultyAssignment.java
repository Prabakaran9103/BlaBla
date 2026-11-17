package edu.iitm.cse.nodues.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faculty_assignments")
public class FacultyAssignment {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false, length = 36)
    private String facultyUserId;

    @Column(nullable = false)
    private int displayPosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacultyDecision currentDecision = FacultyDecision.PENDING;

    private Instant decisionAt;

    protected FacultyAssignment() {
    }

    public FacultyAssignment(String requestId, String facultyUserId, int displayPosition) {
        this.requestId = requestId;
        this.facultyUserId = facultyUserId;
        this.displayPosition = displayPosition;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getFacultyUserId() {
        return facultyUserId;
    }

    public int getDisplayPosition() {
        return displayPosition;
    }

    public FacultyDecision getCurrentDecision() {
        return currentDecision;
    }

    public Instant getDecisionAt() {
        return decisionAt;
    }

    public void decide(FacultyDecision decision) {
        if (decision == FacultyDecision.PENDING) {
            throw new IllegalArgumentException("A decision must be approved or rejected");
        }
        currentDecision = decision;
        decisionAt = Instant.now();
    }

    public void withdraw() {
        currentDecision = FacultyDecision.PENDING;
        decisionAt = Instant.now();
    }
}
