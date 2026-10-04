package th.ac.kku.freelance_hub.service;

import java.util.UUID;

/** Supplies the authenticated owner's identity without exposing a persistence entity. */
public interface CurrentUserProvider {
    UUID currentUserId();
}
