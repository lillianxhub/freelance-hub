package th.ac.kku.freelance_hub.dto.request.project;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class ProjectTaskReorderRequest {

    @NotNull(message = "กรุณาระบุงานย่อยที่ต้องการย้าย")
    private UUID taskId;

    @NotNull(message = "กรุณาระบุลำดับงาน")
    @PositiveOrZero(message = "ลำดับงานต้องไม่ติดลบ")
    @Schema(description = "ตำแหน่งใหม่ในรายการงานที่ยังใช้งาน เริ่มจาก 0", example = "0")
    private Integer sortOrder;
}
