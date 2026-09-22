package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

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

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        Long ownerId = userService.getCurrentUserEntity().getId();
        ClientResponse response = clientService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/clients/" + response.getId())).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ClientResponse>> list(@Valid @ModelAttribute ClientFilterRequest filter) {
        Long ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(clientService.list(ownerId, filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponse> getById(@PathVariable UUID id) {
        Long ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(clientService.getById(ownerId, id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateClientRequest request
    ) {
        Long ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(clientService.update(ownerId, id, request));
    }

    /** Soft-delete: preserve the client and its history by marking it archived. */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        Long ownerId = userService.getCurrentUserEntity().getId();
        clientService.archive(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
