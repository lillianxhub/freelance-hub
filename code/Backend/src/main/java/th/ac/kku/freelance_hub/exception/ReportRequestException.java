package th.ac.kku.freelance_hub.exception;

import org.springframework.http.HttpStatus;

/** Report errors retain their existing HTTP-derived event codes. */
public class ReportRequestException extends ApiException {
    public ReportRequestException(HttpStatus status, String message) {
        super(status, status.name(), message, null);
    }
}
