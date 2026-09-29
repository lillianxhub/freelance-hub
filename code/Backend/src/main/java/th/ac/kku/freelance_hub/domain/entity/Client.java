package th.ac.kku.freelance_hub.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import th.ac.kku.freelance_hub.domain.enums.ClientStatus;
import th.ac.kku.freelance_hub.domain.valueobject.Address;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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

/** A client managed by a freelancer. */
@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_clients_owner_is_active", columnList = "owner_id,is_active"),
    @Index(name = "idx_clients_owner_name", columnList = "owner_id,name"),
    @Index(name = "idx_clients_email", columnList = "email"),
    @Index(name = "idx_clients_province", columnList = "province"),
    @Index(name = "idx_clients_postal_code", columnList = "postal_code")
}, uniqueConstraints = @UniqueConstraint(name = "uk_clients_id_owner", columnNames = {"id", "owner_id"}))
public class Client {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_clients_owner"))
    private User owner;

    @Column(nullable = false, length = 150) private String name;
    @Column(name = "company_name", length = 200) private String companyName;
    @Column(length = 254) private String email;
    @Column(length = 30) private String phone;
    @Embedded private Address addressDetails;
    @Column(name = "tax_id", length = 30) private String taxId;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private Long version;

    protected Client() { }

    public Client(User owner, String name) {
        this.owner = Objects.requireNonNull(owner, "owner is required");
        this.name = requireName(name);
    }

    public void updateDetailsWithAddress(String name, String companyName, String email, String phone,
            String address, String subdistrict, String district, String province, String postalCode,
            String taxId, String notes) {
        this.name = requireName(name);
        this.companyName = trimToNull(companyName);
        this.email = trimToNull(email);
        this.phone = trimToNull(phone);
        this.addressDetails = Address.ofNullable(address, subdistrict, district, province, postalCode);
        this.taxId = trimToNull(taxId);
        this.notes = trimToNull(notes);
    }

    public void updateDetails(String name, String companyName, String email, String phone, String address,
            String taxId, String notes) {
        updateDetailsWithAddress(name, companyName, email, phone, address, null, null, null, null, taxId, notes);
    }

    public void archive() { isActive = false; deletedAt = Instant.now(); }
    public void activate() { isActive = true; deletedAt = null; }

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("name is required");
        return value.trim();
    }
    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public UUID getId() { return id; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public String getCompanyName() { return companyName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Address getAddressDetails() { return addressDetails; }
    public String getAddress() { return addressDetails == null ? null : addressDetails.getAddress(); }
    public String getSubdistrict() { return addressDetails == null ? null : addressDetails.getSubdistrict(); }
    public String getDistrict() { return addressDetails == null ? null : addressDetails.getDistrict(); }
    public String getProvince() { return addressDetails == null ? null : addressDetails.getProvince(); }
    public String getPostalCode() { return addressDetails == null ? null : addressDetails.getPostalCode(); }
    public String getTaxId() { return taxId; }
    public String getNotes() { return notes; }
    public ClientStatus getStatus() { return Boolean.TRUE.equals(isActive) ? ClientStatus.ACTIVE : ClientStatus.ARCHIVED; }
    public Boolean getIsActive() { return isActive; }
    public Instant getDeletedAt() { return deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
