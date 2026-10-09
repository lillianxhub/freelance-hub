package th.ac.kku.freelance_hub.dto.request.task;

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

    @NotNull(message = "กรุณาระบุลำดับงาน")
    @PositiveOrZero(message = "ลำดับงานต้องไม่ติดลบ")
    private Integer sortOrder;
}