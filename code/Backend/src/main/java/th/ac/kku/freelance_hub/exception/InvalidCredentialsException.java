package th.ac.kku.freelance_hub.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() { this("อีเมลหรือรหัสผ่านไม่ถูกต้อง"); }
    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message, null);
    }
}
