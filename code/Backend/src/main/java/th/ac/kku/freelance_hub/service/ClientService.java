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

    Page<ClientResponse> list(UUID ownerId, ClientFilterRequest filter);

    ClientResponse update(UUID ownerId, UUID clientId, UpdateClientRequest request);

    /** Archive a client while preserving its project and time history. */
    void archive(UUID ownerId, UUID clientId);
}
