package th.ac.kku.freelance_hub.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Credentials required to change the authenticated user's password. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "กรุณาระบุรหัสผ่านเดิม")
    private String oldPassword;

    @NotBlank(message = "กรุณาระบุรหัสผ่านใหม่")
    @Size(min = 8, max = 100, message = "รหัสผ่านใหม่ต้องมี 8–100 ตัวอักษร")
    private String newPassword;
}
