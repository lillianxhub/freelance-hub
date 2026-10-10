package th.ac.kku.freelance_hub.exception.handling;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationErrorHandler implements ErrorHandler {
    public int getOrder() { return 300; }
    public Optional<ErrorDescriptor> handle(Exception exception, ErrorContext context) {
        if (context.source() == ErrorContext.Source.FILTER_DEPENDENCY
                || exception instanceof AuthenticationServiceException) return Optional.empty();
        if (exception instanceof AccessDeniedException)
            return Optional.of(ErrorDescriptor.of(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "คุณไม่มีสิทธิ์ดำเนินการนี้"));
        if (exception instanceof AuthenticationException) {
            boolean entry = context.source() == ErrorContext.Source.AUTHENTICATION;
            return Optional.of(ErrorDescriptor.of(HttpStatus.UNAUTHORIZED,
                    entry ? "AUTHENTICATION_REQUIRED" : "INVALID_CREDENTIALS",
                    entry ? "กรุณาเข้าสู่ระบบก่อนดำเนินการ" : "อีเมลหรือรหัสผ่านไม่ถูกต้อง"));
        }
        return Optional.empty();
    }
}
