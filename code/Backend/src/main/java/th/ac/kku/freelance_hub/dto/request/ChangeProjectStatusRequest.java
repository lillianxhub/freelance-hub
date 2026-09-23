package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeProjectStatusRequest {

    @NotNull(message = "Project status is required")
    private ProjectStatus status;
}