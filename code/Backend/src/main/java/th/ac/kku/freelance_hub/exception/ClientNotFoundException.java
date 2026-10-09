package th.ac.kku.freelance_hub.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ClientNotFoundException extends ApiException {
    public ClientNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "ไม่พบลูกค้า", Map.of("id", id));
    }
}
