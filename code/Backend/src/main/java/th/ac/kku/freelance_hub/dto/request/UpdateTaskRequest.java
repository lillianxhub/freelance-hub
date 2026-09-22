package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    @NotBlank(message = "Task name is required")
    @Size(
            max = 180,
            message = "Task name must not exceed 180 characters"
    )
    private String name;

    private String description;
}