package th.ac.kku.freelance_hub.dto.request.report;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;
import org.springframework.format.annotation.DateTimeFormat;
import th.ac.kku.freelance_hub.domain.enums.ProjectStatus;

@Data
public class ReportFilterRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private UUID clientId;
    private UUID projectId;
    private ProjectStatus status;

    @AssertTrue(message = "กรุณาระบุวันที่เริ่มและวันที่สิ้นสุดให้ครบ และวันที่เริ่มต้องไม่เกินวันที่สิ้นสุด")
    @JsonIgnore
    public boolean isDateRangeValid() {
        return (from == null && to == null)
                || (from != null && to != null && !from.isAfter(to));
    }
}