package th.ac.kku.freelance_hub.exception;

import java.util.UUID;

/** Same response for a missing client and a client owned by another user. */
public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(UUID id) {
        super("Client not found with id: " + id);
    }
}
