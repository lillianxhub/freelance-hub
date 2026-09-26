package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fields to update on an existing client.
 * A null field means no change. An empty optional field clears its stored value.
 * Status changes use the archive/activate operations, not this request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateClientRequest {

    @Pattern(regexp = ".*\\S.*", message = "Client name must not be blank")
    @Size(max = 150, message = "Client name must not exceed 150 characters")
    private String name;

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @Pattern(
        regexp = "^\\s*$|^\\+?[0-9() .-]{6,30}$",
        message = "Phone must contain only digits and common phone separators"
    )
    @Size(max = 30, message = "Phone must not exceed 30 characters")
    private String phone;

    private String address;

    @Size(max = 100, message = "Subdistrict must not exceed 100 characters")
    private String subdistrict;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 30, message = "Tax ID must not exceed 30 characters")
    private String taxId;

    private String notes;
}
