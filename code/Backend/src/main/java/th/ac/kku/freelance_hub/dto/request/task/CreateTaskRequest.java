package th.ac.kku.freelance_hub.dto.request.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "กรุณาระบุชื่องาน")
    @Size(
            max = 180,
            message = "ชื่องานต้องไม่เกิน 180 ตัวอักษร"
    )
    private String name;

    private String description;

    @NotNull(message = "กรุณาระบุลำดับงาน")
    @PositiveOrZero(message = "ลำดับงานต้องไม่ติดลบ")
    private Integer sortOrder;
}