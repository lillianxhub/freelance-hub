package th.ac.kku.freelance_hub.domain.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Normalized address shared by user profiles and clients. */
@Entity
@Table(
    name = "addresses",
    indexes = {
        @Index(name = "idx_addresses_province", columnList = "province"),
        @Index(name = "idx_addresses_postal_code", columnList = "postal_code")
    }
)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "text")
    private String address;

    @Column(length = 100)
    private String subdistrict;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String province;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Address() {
    }

    public Address(String address, String subdistrict, String district, String province, String postalCode) {
        update(address, subdistrict, district, province, postalCode);
    }

    public void update(String address, String subdistrict, String district, String province, String postalCode) {
        this.address = trimToNull(address);
        this.subdistrict = trimToNull(subdistrict);
        this.district = trimToNull(district);
        this.province = trimToNull(province);
        this.postalCode = trimToNull(postalCode);
    }

    public UUID getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getSubdistrict() {
        return subdistrict;
    }

    public String getDistrict() {
        return district;
    }

    public String getProvince() {
        return province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
