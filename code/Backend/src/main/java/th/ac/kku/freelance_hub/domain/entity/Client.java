package th.ac.kku.freelance_hub.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.ClientStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * A client managed by a freelancer.
 *
 * <p>Every client belongs to exactly one user. Clients are archived rather than
 * deleted so their project and time history can be preserved.</p>
 */
@Entity
@Table(
    name = "clients",
    indexes = {
        @Index(name = "idx_clients_owner_status", columnList = "owner_id,status"),
        @Index(name = "idx_clients_owner_name", columnList = "owner_id,name"),
        @Index(name = "idx_clients_email", columnList = "email")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_clients_id_owner",
            columnNames = {"id", "owner_id"}
        )
    }
)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "owner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_clients_owner")
    )
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(length = 254)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Column(columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Client() {
    }

    public Client(User owner, String name) {
        this.owner = Objects.requireNonNull(owner, "owner is required");
        this.name = requireName(name);
    }

    public void updateDetails(
        String name,
        String companyName,
        String email,
        String phone,
        String address,
        String taxId,
        String notes
    ) {
        this.name = requireName(name);
        this.companyName = trimToNull(companyName);
        this.email = trimToNull(email);
        this.phone = trimToNull(phone);
        this.address = trimToNull(address);
        this.taxId = trimToNull(taxId);
        this.notes = trimToNull(notes);
    }

    /** Marks this client as archived without deleting its historical data. */
    public void archive() {
        status = ClientStatus.ARCHIVED;
    }

    /** Restores an archived client. */
    public void activate() {
        status = ClientStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        return name.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getNotes() {
        return notes;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
