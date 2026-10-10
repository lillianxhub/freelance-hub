package th.ac.kku.freelance_hub.dto.request.project;

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
public class UpdateProjectRequest {

    @NotNull(message = "กรุณาระบุลูกค้า")
    private UUID clientId;

    @NotBlank(message = "กรุณาระบุชื่อโปรเจกต์")
    @Size(
            max = 180,
            message = "ชื่อโปรเจกต์ต้องไม่เกิน 180 ตัวอักษร"
    )
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "กรุณาระบุสีในรูปแบบ #RRGGBB"
    )
    private String color;

    @Positive(message = "เวลาเป้าหมายต้องมากกว่าศูนย์")
    private Integer targetMinutes;
}