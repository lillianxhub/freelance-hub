package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateClientRequest;
import th.ac.kku.freelance_hub.dto.response.ClientResponse;
import th.ac.kku.freelance_hub.service.ClientService;
import th.ac.kku.freelance_hub.service.UserService;

@Tag(name = "Clients", description = "Manage clients belonging to the authenticated user")
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;

    @Operation(summary = "Create a client", description = "Create a client for the authenticated user")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        ClientResponse response = clientService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/clients/" + response.getId())).body(response);
    }

    @Operation(summary = "List clients", description = "List the authenticated user's clients with optional filters, sorting, and pagination")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ClientResponse>> list(@Valid @ModelAttribute ClientFilterRequest filter) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(clientService.list(ownerId, filter));
    }

    @Operation(summary = "Get a client", description = "Get one client belonging to the authenticated user by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponse> getById(@PathVariable UUID id) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(clientService.getById(ownerId, id));
    }

    @Operation(summary = "Update a client", description = "Update fields of a client belonging to the authenticated user")
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateClientRequest request
    ) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(clientService.update(ownerId, id, request));
    }

    /** Soft-delete: preserve the client and its history by marking it archived. */
    @Operation(summary = "Archive a client", description = "Soft-delete a client belonging to the authenticated user while preserving its history")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        clientService.archive(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
