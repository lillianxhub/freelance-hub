package th.ac.kku.freelance_hub.dto.request.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Optional fields that can be changed on the authenticated user's profile. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequest {

    @Pattern(regexp = ".*\\S.*", message = "ชื่อที่แสดงต้องไม่เป็นช่องว่าง")
    @Size(max = 255, message = "ชื่อที่แสดงต้องไม่เกิน 255 ตัวอักษร")
    private String displayName;

    @Size(max = 100, message = "ชื่อต้องไม่เกิน 100 ตัวอักษร")
    private String firstName;

    @Size(max = 100, message = "นามสกุลต้องไม่เกิน 100 ตัวอักษร")
    private String lastName;

    @Pattern(
        regexp = "^\\s*$|^\\+?[0-9() .-]{6,20}$",
        message = "เบอร์โทรศัพท์ต้องมีเฉพาะตัวเลขหรือเครื่องหมายคั่นที่ใช้ทั่วไป"
    )
    private String phone;

    @Size(max = 500, message = "ที่อยู่ต้องไม่เกิน 500 ตัวอักษร")
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

    @Size(max = 1000, message = "ประวัติย่อต้องไม่เกิน 1000 ตัวอักษร")
    private String bio;
}
