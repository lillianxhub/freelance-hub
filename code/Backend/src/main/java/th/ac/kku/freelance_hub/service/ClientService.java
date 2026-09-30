package th.ac.kku.freelance_hub.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;

/** Business operations for clients owned by the authenticated freelancer. */
public interface ClientService {

    /** The owner ID must come from authentication, never from request data. */
    ClientResponse create(UUID ownerId, CreateClientRequest request);

    ClientResponse getById(UUID ownerId, UUID clientId);

    /** Include selected Project fields and, optionally, their Task fields. */
    ClientResponse getById(UUID ownerId, UUID clientId, boolean includeProjects, boolean includeTasks);

    Page<ClientResponse> list(UUID ownerId, ClientFilterRequest filter);

    /** Replace all editable client details; omitted optional fields are cleared. */
    ClientResponse replace(UUID ownerId, UUID clientId, CreateClientRequest request);

    ClientResponse update(UUID ownerId, UUID clientId, UpdateClientRequest request);

    /** Change the client's active status without soft-deleting it. */
    ClientResponse changeStatus(UUID ownerId, UUID clientId, boolean isActive);

    /** Soft-delete a client while preserving its active status and history. */
    void softDelete(UUID ownerId, UUID clientId);
}
