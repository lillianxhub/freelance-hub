package th.ac.kku.freelance_hub.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 255)
    private String displayName;

    @Column(length = 255)
    private String firstName;

    @Column(length = 255)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 255)
    private String city;

    @Column(length = 255)
    private String country;

    @Column(length = 20)
    private String postalCode;

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
