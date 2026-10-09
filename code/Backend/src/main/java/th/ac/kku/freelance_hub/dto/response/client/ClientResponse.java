package th.ac.kku.freelance_hub.dto.response.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private String subdistrict;
    private String district;
    private String province;
    private String postalCode;
    private String taxId;
    private String notes;
    private ClientStatus status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    /** Calculated for GET responses; omitted from write responses. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "All-time completed time entry seconds, using the time entry summary rules; returned by Client GET endpoints",
            example = "5400", accessMode = Schema.AccessMode.READ_ONLY)
    private Long totalTrackedSeconds;

    /** Absent unless the caller requests include=projects or include=projects.tasks. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Returned only by GET /api/clients/{id} when include=projects or include=projects.tasks; each project's tasks are returned only for include=projects.tasks", accessMode = Schema.AccessMode.READ_ONLY)
    private List<ClientProjectSummaryResponse> projects;
}
