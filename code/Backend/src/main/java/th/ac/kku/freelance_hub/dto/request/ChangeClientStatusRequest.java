package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Desired active state for a client's soft-delete lifecycle. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeClientStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
