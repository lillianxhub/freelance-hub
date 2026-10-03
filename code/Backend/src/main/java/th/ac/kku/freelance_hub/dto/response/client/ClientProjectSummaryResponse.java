package th.ac.kku.freelance_hub.dto.response.client;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

/** Selected Project fields included with a Client detail on request. */
public record ClientProjectSummaryResponse(
        UUID id,
        String name,
        String color,
        ProjectStatus status,
        Integer targetMinutes,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<ClientTaskSummaryResponse> tasks) {
}
