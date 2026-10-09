package th.ac.kku.freelance_hub.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationRequiredException extends ApiException {
    public AuthenticationRequiredException() {
        super(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "กรุณาเข้าสู่ระบบก่อนดำเนินการ", null);
    }
}
