package th.ac.kku.freelance_hub.dto.request.project;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

@Data //ให้ Lombok สร้าง getter/setter
@Builder //ช่วยสร้าง object
@NoArgsConstructor
@AllArgsConstructor
public class ChangeProjectStatusRequest {

    @NotNull(message = "Project status is required")
    private ProjectStatus status;
}