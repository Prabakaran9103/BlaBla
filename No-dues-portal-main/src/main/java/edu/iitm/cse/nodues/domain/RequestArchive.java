package edu.iitm.cse.nodues.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "request_archives")
public class RequestArchive {

    @Id
    @Column(length = 73, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false)
    private boolean archived;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RequestArchive() {
    }

    public RequestArchive(String userId, String requestId) {
        this.userId = userId;
        this.requestId = requestId;
    }

    @PrePersist
    void onCreate() {
        id = userId + ":" + requestId;
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRequestId() { return requestId; }
    public boolean isArchived() { return archived; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setArchived(boolean archived) {
        this.archived = archived;
        this.updatedAt = Instant.now();
    }
}
