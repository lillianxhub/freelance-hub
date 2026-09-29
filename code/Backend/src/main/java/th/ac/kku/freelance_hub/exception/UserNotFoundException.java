package th.ac.kku.freelance_hub.exception;

import java.util.UUID;

/**
 * Exception thrown when user is not found
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("ไม่พบผู้ใช้รหัส: " + id);
    }

    public UserNotFoundException(String email) {
        super("ไม่พบผู้ใช้อีเมล: " + email);
    }
}
