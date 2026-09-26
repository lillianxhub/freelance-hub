package th.ac.kku.freelance_hub.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserProfile entity containing user's personal information.
 * Has One-to-One relationship with User.
 */
@Entity
@Table(name = "user_profiles")
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

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(length = 255)
    private String firstName;

    @Column(length = 255)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(
        name = "address_id",
        foreignKey = @ForeignKey(name = "fk_user_profiles_address")
    )
    private Address address;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "Asia/Bangkok";

    @Column(length = 20)
    @Builder.Default
    private String dateFormat = "dd/MM/yyyy";

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 1000)
    private String bio;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
