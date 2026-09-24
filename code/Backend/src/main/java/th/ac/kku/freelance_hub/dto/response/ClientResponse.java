package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;

/** Client data returned by the API without exposing the JPA entity. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {

    private UUID id;
    private String name;
    private String companyName;
    private String email;
    private String phone;
    private String address;
    private String taxId;
    private String notes;
    private ClientStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
