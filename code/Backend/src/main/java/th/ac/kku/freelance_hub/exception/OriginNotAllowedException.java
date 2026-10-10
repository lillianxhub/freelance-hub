package th.ac.kku.freelance_hub.exception;

import org.springframework.http.HttpStatus;

public class OriginNotAllowedException extends ApiException {
    public OriginNotAllowedException() {
        super(HttpStatus.FORBIDDEN, "ORIGIN_NOT_ALLOWED", "ไม่อนุญาตให้ส่งคำขอจากเว็บไซต์นี้", null);
    }
}
