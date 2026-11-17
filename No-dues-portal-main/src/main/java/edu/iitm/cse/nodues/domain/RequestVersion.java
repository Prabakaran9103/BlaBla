package edu.iitm.cse.nodues.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_versions")
public class RequestVersion {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, length = 36)
    private String submittedByUserId;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected RequestVersion() {
    }

    public RequestVersion(String requestId, int versionNumber, String details, String submittedByUserId) {
        this.requestId = requestId;
        this.versionNumber = versionNumber;
        this.details = details;
        this.submittedByUserId = submittedByUserId;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        submittedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getDetails() {
        return details;
    }

    public String getSubmittedByUserId() {
        return submittedByUserId;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
