package th.ac.kku.freelance_hub.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
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
}
