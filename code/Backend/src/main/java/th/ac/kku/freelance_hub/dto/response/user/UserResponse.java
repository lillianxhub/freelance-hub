package th.ac.kku.freelance_hub.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.UserRole;

import java.time.Instant;
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
    private Boolean isActive;
    private Instant createdAt;

    // Profile fields
    private String displayName;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String subdistrict;
    private String district;
    private String province;
    private String postalCode;
    private String taxId;
    private String bio;
}
