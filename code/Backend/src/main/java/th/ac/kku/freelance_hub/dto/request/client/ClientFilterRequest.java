package th.ac.kku.freelance_hub.dto.request.client;

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
import org.springframework.data.domain.Sort;
import th.ac.kku.freelance_hub.domain.enums.ClientStatus;

/** Filters and pagination options for listing the current user's clients. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientFilterRequest {

    /** Null includes both active and archived clients. */
    private ClientStatus status;

    /**
     * Optional prefix search over name, company, email, phone, and address; blank
     * means no search.
     */
    @Size(max = 150, message = "คำค้นหาต้องไม่เกิน 150 ตัวอักษร")
    private String search;

    /** One-based page number exposed by the API. */
    @Builder.Default
    @Min(value = 1, message = "หมายเลขหน้าต้องไม่น้อยกว่า 1")
    private int page = 1;

    @Builder.Default
    @Min(value = 1, message = "จำนวนรายการต่อหน้าต้องไม่น้อยกว่า 1")
    @Max(value = 100, message = "จำนวนรายการต่อหน้าต้องไม่เกิน 100")
    private int size = 20;

    /** Preferred page-size parameter; takes precedence over the legacy size parameter. */
    @Min(value = 1, message = "จำนวนรายการต่อหน้าต้องไม่น้อยกว่า 1")
    @Max(value = 100, message = "จำนวนรายการต่อหน้าต้องไม่เกิน 100")
    private Integer limit;

    /** Allow only known Client properties as sort keys. */
    @Builder.Default
    @NotBlank(message = "กรุณาระบุฟิลด์ที่ใช้เรียงลำดับ")
    @Pattern(regexp = "name|companyName|email|createdAt|updatedAt", message = "ฟิลด์ที่ใช้เรียงลำดับไม่ถูกต้อง")
    private String sortBy = "name";

    @Builder.Default
    @NotNull(message = "กรุณาระบุทิศทางการเรียงลำดับ")
    private Sort.Direction direction = Sort.Direction.ASC;
}
