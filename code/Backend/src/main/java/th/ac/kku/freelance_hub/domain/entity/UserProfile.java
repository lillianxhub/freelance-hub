package th.ac.kku.freelance_hub.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * UserProfile entity containing user's personal information.
 * Has One-to-One relationship with User.
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_profiles_province", columnList = "province"),
    @Index(name = "idx_user_profiles_postal_code", columnList = "postal_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String displayName;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

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

    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Column(length = 20)
    @Builder.Default
    private String dateFormat = "YYYY-MM-DD";

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
