package th.ac.kku.freelance_hub.dto.request.auth;

import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import th.ac.kku.freelance_hub.security.EmailNormalizer;

/**
 * DTO for user registration request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "กรุณาระบุอีเมล")
    private String email;

    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 255, message = "อีเมลต้องไม่เกิน 255 ตัวอักษร")
    public String getEmail() {
        return email == null ? null : EmailNormalizer.normalize(email);
    }

    public void setEmail(String email) {
        this.email = email == null ? null : EmailNormalizer.normalize(email);
    }

    @NotBlank(message = "กรุณาระบุรหัสผ่าน")
    @Size(min = 8, max = 100, message = "รหัสผ่านต้องมี 8–100 ตัวอักษร")
    private String password;

    @AssertTrue(message = "รหัสผ่านต้องไม่เกิน 72 ไบต์ UTF-8")
    @JsonIgnore
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @NotBlank(message = "กรุณาระบุชื่อที่แสดง")
    @Size(max = 255, message = "ชื่อที่แสดงต้องไม่เกิน 255 ตัวอักษร")
    private String displayName;

    @Size(max = 100, message = "ชื่อต้องไม่เกิน 100 ตัวอักษร")
    private String firstName;

    @Size(max = 100, message = "นามสกุลต้องไม่เกิน 100 ตัวอักษร")
    private String lastName;

    @Size(max = 20, message = "เบอร์โทรศัพท์ต้องไม่เกิน 20 ตัวอักษร")
    private String phone;

}
