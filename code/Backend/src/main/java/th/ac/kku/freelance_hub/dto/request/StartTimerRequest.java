package th.ac.kku.freelance_hub.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data submitted when starting a timer.
 *
 * <p>The owner and start time are determined by the server from the
 * authenticated user and the server clock.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartTimerRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID taskId;

    private String description;
}
