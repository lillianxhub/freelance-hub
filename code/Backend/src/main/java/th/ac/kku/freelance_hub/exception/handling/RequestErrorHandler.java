package th.ac.kku.freelance_hub.exception.handling;

import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Component
public class RequestErrorHandler implements ErrorHandler {
    public int getOrder() { return 400; }
    public Optional<ErrorDescriptor> handle(Exception exception, ErrorContext context) {
        if (context.source() != ErrorContext.Source.MVC) return Optional.empty();
        if (exception instanceof HttpMessageNotReadableException)
            return Optional.of(ErrorDescriptor.of(HttpStatus.BAD_REQUEST,
                    context.isAuthOrClient() ? "INVALID_REQUEST_BODY" : "INVALID_REQUEST", "ข้อมูลที่ส่งมาไม่ถูกต้อง"));
        if (exception instanceof MethodArgumentTypeMismatchException ex)
            return Optional.of(new ErrorDescriptor(HttpStatus.BAD_REQUEST,
                    context.isClient() ? "INVALID_ARGUMENT" : "INVALID_REQUEST", "พารามิเตอร์ไม่ถูกต้อง",
                    Map.of("field", ex.getName()), null, null));
        if (exception instanceof ErrorResponse ex) {
            var status = ex.getStatusCode();
            HttpStatus standard = HttpStatus.resolve(status.value());
            String code = standard == null ? "HTTP_" + status.value() : standard.name();
            String message = switch (status.value()) {
                case 400 -> "ข้อมูลที่ส่งมาไม่ถูกต้อง";
                case 401 -> "กรุณาเข้าสู่ระบบก่อนดำเนินการ";
                case 403 -> "คุณไม่มีสิทธิ์ดำเนินการนี้";
                case 404 -> "ไม่พบข้อมูลที่ร้องขอ";
                case 405 -> "ไม่รองรับวิธีส่งคำขอนี้";
                case 406 -> "ไม่สามารถส่งข้อมูลในรูปแบบที่ร้องขอได้";
                case 409 -> "ไม่สามารถดำเนินการได้เนื่องจากสถานะปัจจุบัน";
                case 413 -> "ข้อมูลที่ส่งมามีขนาดใหญ่เกินไป";
                case 415 -> "ไม่รองรับชนิดข้อมูลที่ส่งมา";
                case 429 -> "ส่งคำขอบ่อยเกินไป กรุณารอสักครู่";
                default -> status.is5xxServerError() ? "เกิดข้อผิดพลาดภายในระบบ" : "ไม่สามารถดำเนินการตามคำขอได้";
            };
            return Optional.of(new ErrorDescriptor(status, code, message, null, null, ex.getHeaders()));
        }
        return Optional.empty();
    }
}
