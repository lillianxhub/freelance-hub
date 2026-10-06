package th.ac.kku.freelance_hub.dto.request.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReportProjectsRequest {
    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int limit = 10;

    @Pattern(
            regexp = "projectName|clientName|trackedSeconds|usagePercent",
            message = "sortBy ไม่ถูกต้อง"
    )
    private String sortBy = "projectName";

    @Pattern(
            regexp = "asc|desc",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "direction ต้องเป็น asc หรือ desc"
    )
    private String direction = "asc";
}