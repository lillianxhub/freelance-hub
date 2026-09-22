package th.ac.kku.freelance_hub.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotBlank(message = "Project name is required")
    @Size(
            max = 180,
            message = "Project name must not exceed 180 characters"
    )
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "Color must use #RRGGBB format"
    )
    private String color;

    @Positive(message = "Target minutes must be greater than zero")
    private Integer targetMinutes;
}