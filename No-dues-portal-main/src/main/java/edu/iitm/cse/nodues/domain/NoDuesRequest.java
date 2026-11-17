package edu.iitm.cse.nodues.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "no_dues_requests")
public class NoDuesRequest {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String studentId;

    @Column(nullable = false, length = 36)
    private String facultyAdvisorId;

    @Column(length = 36)
    private String assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestStatus status;

    @Column(nullable = false)
    private int currentVersionNumber;

    @Column(length = 36)
    private String deletedByUserId;

    private Instant deletedAt;
    private Instant forwardedAt;
    private Instant closedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private long lockVersion;

    protected NoDuesRequest() {
    }

    public NoDuesRequest(String studentId, String facultyAdvisorId) {
        this.studentId = studentId;
        this.facultyAdvisorId = facultyAdvisorId;
        this.status = RequestStatus.WAITING_FOR_STAFF;
        this.currentVersionNumber = 1;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFacultyAdvisorId() {
        return facultyAdvisorId;
    }

    public String getAssignedStaffId() {
        return assignedStaffId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public int getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    public String getDeletedByUserId() {
        return deletedByUserId;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getForwardedAt() {
        return forwardedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }

    public void forwardTo(String staffId) {
        if (status != RequestStatus.WAITING_FOR_STAFF) {
            throw new IllegalStateException("Only new requests can be forwarded");
        }
        assignedStaffId = staffId;
        forwardedAt = Instant.now();
        status = RequestStatus.IN_FACULTY_REVIEW;
    }

    public void requestRevision() {
        status = RequestStatus.REVISION_REQUESTED;
    }

    public void resubmit() {
        if (status != RequestStatus.REVISION_REQUESTED) {
            throw new IllegalStateException("Only rejected requests can be resubmitted");
        }
        currentVersionNumber++;
        status = RequestStatus.IN_FACULTY_REVIEW;
    }

    public void readyForClosure() {
        status = RequestStatus.AWAITING_STAFF_CLOSURE;
    }

    public void close() {
        if (status != RequestStatus.AWAITING_STAFF_CLOSURE) {
            throw new IllegalStateException("Request is not ready for closure");
        }
        status = RequestStatus.CLOSED;
        closedAt = Instant.now();
    }

    public void delete(String deletedByUserId) {
        this.deletedByUserId = deletedByUserId;
        deletedAt = Instant.now();
        status = RequestStatus.DELETED;
    }

    public void restore() {
        if (status != RequestStatus.DELETED) {
            throw new IllegalStateException("Only deleted requests can be restored");
        }
        deletedByUserId = null;
        deletedAt = null;
        status = RequestStatus.WAITING_FOR_STAFF;
    }
}
