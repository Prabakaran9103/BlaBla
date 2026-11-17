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
@Table(name = "request_files")
public class RequestFile {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String requestId;

    @Column(nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestFileType fileType;

    @Column(nullable = false, length = 36)
    private String uploaderId;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 500)
    private String storageReference;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    protected RequestFile() {
    }

    public RequestFile(
            String requestId,
            int versionNumber,
            RequestFileType fileType,
            String uploaderId,
            String originalFileName,
            long fileSize,
            String storageReference) {
        this.requestId = requestId;
        this.versionNumber = versionNumber;
        this.fileType = fileType;
        this.uploaderId = uploaderId;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.storageReference = storageReference;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        uploadedAt = Instant.now();
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

    public RequestFileType getFileType() {
        return fileType;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
