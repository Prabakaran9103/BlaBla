package edu.iitm.cse.nodues.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String token;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private Instant expiresAt;

    protected PasswordResetToken() {}

    public PasswordResetToken(String userId) {
        this.token = UUID.randomUUID().toString();
        this.userId = userId;
        this.expiresAt = Instant.now().plusSeconds(3600);
    }

    public String getToken() { return token; }
    public String getUserId() { return userId; }
    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
}
