package th.ac.kku.freelance_hub.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private UUID id;

    // private UUID ownerId;

    private UUID clientId;

    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private String color;

    private Integer targetMinutes;

    private ProjectStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    private Long version;
}