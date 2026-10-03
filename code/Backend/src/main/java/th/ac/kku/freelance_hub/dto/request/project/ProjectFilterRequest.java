package th.ac.kku.freelance_hub.dto.request.project;

import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFilterRequest {

    @Size(max = 180, message = "คำค้นหาต้องไม่เกิน 180 ตัวอักษร")
    private String search;

    @Pattern(regexp = "ALL|PLANNED|ACTIVE|ON_HOLD|COMPLETED|ARCHIVED",
            message = "status ไม่ถูกต้อง")
    private String status;

    @Pattern(regexp = "tasks", message = "include รองรับเฉพาะ tasks")
    private String include;

    @Builder.Default
    @Min(value = 1, message = "หน้าต้องเริ่มจาก 1")
    private int page = 1;

    @Builder.Default
    @Min(value = 1, message = "จำนวนรายการต่อหน้าต้องไม่น้อยกว่า 1")
    @Max(value = 100, message = "จำนวนรายการต่อหน้าต้องไม่เกิน 100")
    private int limit = 20;

    @Builder.Default
    @NotBlank(message = "ต้องระบุฟิลด์ที่ใช้เรียงลำดับ")
    @Pattern(
            regexp = "update_at|end_date|project_name",
            message = "เรียงลำดับได้ด้วย update_at, end_date หรือ project_name"
    )
    private String sortBy = "project_name";

    @Builder.Default
    @NotNull(message = "ต้องระบุทิศทางการเรียงลำดับ")
    private Sort.Direction direction = Sort.Direction.ASC;
}
