package th.ac.kku.freelance_hub.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A JWT identifier that must no longer be accepted. */
@Entity
@Table(
        name = "revoked_tokens",
        indexes = {
                @Index(name = "idx_revoked_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_revoked_tokens_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_revoked_tokens_jti", columnNames = "jti")
        }
)
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 36)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, User user, Instant expiresAt, Instant revokedAt) {
        this.jti = Objects.requireNonNull(jti, "jti is required");
        this.user = Objects.requireNonNull(user, "user is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt is required");
    }

    public UUID getId() {
        return id;
    }

    public String getJti() {
        return jti;
    }

    public User getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
