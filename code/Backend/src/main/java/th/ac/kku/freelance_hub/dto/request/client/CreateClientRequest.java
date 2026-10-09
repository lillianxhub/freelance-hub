package th.ac.kku.freelance_hub.dto.request.client;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data submitted when creating or replacing a client. Ownership comes from authentication. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientRequest {

    @NotBlank(message = "กรุณาระบุชื่อลูกค้า")
    @Size(max = 150, message = "ชื่อลูกค้าต้องไม่เกิน 150 ตัวอักษร")
    private String name;

    @Size(max = 200, message = "ชื่อบริษัทต้องไม่เกิน 200 ตัวอักษร")
    private String companyName;

    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 254, message = "อีเมลต้องไม่เกิน 254 ตัวอักษร")
    private String email;

    @Pattern(
        regexp = "^\\+?[0-9() .-]{6,30}$",
        message = "เบอร์โทรศัพท์ต้องใช้ตัวเลขและตัวคั่นที่รองรับ"
    )
    private String phone;

    private String address;

    @Size(max = 100, message = "ตำบลต้องไม่เกิน 100 ตัวอักษร")
    private String subdistrict;

    @Size(max = 100, message = "อำเภอต้องไม่เกิน 100 ตัวอักษร")
    private String district;

    @Size(max = 100, message = "จังหวัดต้องไม่เกิน 100 ตัวอักษร")
    private String province;

    @Size(max = 20, message = "รหัสไปรษณีย์ต้องไม่เกิน 20 ตัวอักษร")
    private String postalCode;

    @Size(max = 30, message = "เลขประจำตัวผู้เสียภาษีต้องไม่เกิน 30 ตัวอักษร")
    private String taxId;

    private String notes;
}
