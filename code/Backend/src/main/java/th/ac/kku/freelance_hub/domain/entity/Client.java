package th.ac.kku.freelance_hub.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A client managed by a freelancer.
 *
 * <p>Every client belongs to exactly one user. Removing a client is represented
 * by setting {@code active} to {@code false} so its project and time history can
 * be preserved.</p>
 */
@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_clients_owner_id", columnList = "owner_id"),
    @Index(name = "idx_clients_is_active", columnList = "is_active"),
    @Index(name = "idx_clients_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "owner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_client_owner")
    )
    private User owner;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "company", length = 255)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Marks this client as archived without deleting its historical data. */
    public void archive() {
        this.active = false;
    }

    /** Restores an archived client. */
    public void activate() {
        this.active = true;
    }
}
