package th.ac.kku.freelance_hub.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderTaskRequest {

    @NotNull(message = "Sort order is required")
    @PositiveOrZero(message = "Sort order must not be negative")
    private Integer sortOrder;
}