package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Optional fields that can be changed on the authenticated user's profile. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequest {

    @Pattern(regexp = ".*\\S.*", message = "Display name must not be blank")
    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Pattern(
        regexp = "^\\s*$|^\\+?[0-9() .-]{6,20}$",
        message = "Phone must contain only digits and common phone separators"
    )
    private String phone;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 100, message = "Subdistrict must not exceed 100 characters")
    private String subdistrict;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    @Size(max = 20, message = "Date format must not exceed 20 characters")
    private String dateFormat;

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImageUrl;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;
}
