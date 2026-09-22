package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data submitted when creating a client. Ownership is taken from the logged-in user. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientRequest {

    @NotBlank(message = "Client name is required")
    @Size(max = 150, message = "Client name must not exceed 150 characters")
    private String name;

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @Pattern(
        regexp = "^\\+?[0-9() .-]{6,30}$",
        message = "Phone must contain only digits and common phone separators"
    )
    private String phone;

    private String address;

    @Size(max = 30, message = "Tax ID must not exceed 30 characters")
    private String taxId;

    private String notes;
}
