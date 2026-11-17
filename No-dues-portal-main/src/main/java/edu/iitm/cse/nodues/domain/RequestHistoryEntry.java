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
@Table(name = "request_history")
public class RequestHistoryEntry {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false, length = 36)
    private String actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HistoryActionType actionType;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    protected RequestHistoryEntry() {
    }

    public RequestHistoryEntry(
            String requestId,
            String actorUserId,
            HistoryActionType actionType,
            String remark) {
        this.requestId = requestId;
        this.actorUserId = actorUserId;
        this.actionType = actionType;
        this.remark = remark;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        occurredAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public HistoryActionType getActionType() {
        return actionType;
    }

    public String getRemark() {
        return remark;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
