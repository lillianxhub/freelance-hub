package th.ac.kku.freelance_hub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.UserRole;
import th.ac.kku.freelance_hub.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user response with profile information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private UserRole role;
    private UserStatus status;
    private Boolean enabled;
    private LocalDateTime createdAt;

    // Profile fields
    private String displayName;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String postalCode;
    private String avatarUrl;
    private String timezone;
    private String dateFormat;
}
