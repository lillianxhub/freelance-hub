package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.common.response.PaginationMeta;
import th.ac.kku.freelance_hub.dto.request.CreateClientRequest;
import th.ac.kku.freelance_hub.dto.request.ClientFilterRequest;
import th.ac.kku.freelance_hub.dto.request.ChangeClientStatusRequest;
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
    @ApiResponse(responseCode = "201", description = "Client created", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid client data", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<ClientResponse>> create(
            @Valid @RequestBody CreateClientRequest request) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        ClientResponse response = clientService.create(ownerId, request);
        return ResponseEntity.created(URI.create("/api/clients/" + response.getId()))
                .body(ApiResult.success("สร้างลูกค้าสำเร็จ", response));
    }

    @Operation(summary = "List clients", description = "List the authenticated user's clients with optional filters, sorting, and pagination")
    @ApiResponse(responseCode = "200", description = "Page of clients returned", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid filter, sorting, or pagination options", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<List<ClientResponse>>> list(
            @ParameterObject @Valid @ModelAttribute ClientFilterRequest filter) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        Page<ClientResponse> clients = clientService.list(ownerId, filter);
        // Pagination metadata is one-based even though the query parameter is zero-based.
        PaginationMeta meta = PaginationMeta.builder()
                .page(clients.getNumber() + 1)
                .limit(clients.getSize())
                .total(clients.getTotalElements())
                .totalPages(clients.getTotalPages())
                .build();
        return ResponseEntity.ok(ApiResult.success(
                "ดึงรายชื่อลูกค้าสำเร็จ", clients.getContent(), meta));
    }

    @Operation(summary = "Get a client", description = "Get one client belonging to the authenticated user by ID")
    @ApiResponse(responseCode = "200", description = "Client returned", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<ClientResponse>> getById(
            @PathVariable UUID id) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(ApiResult.success(
                "ดึงข้อมูลลูกค้าสำเร็จ", clientService.getById(ownerId, id)));
    }

    @Operation(summary = "Replace a client", description = "Replace editable details of a client belonging to the authenticated user")
    @ApiResponse(responseCode = "200", description = "Client updated", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid client data", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<ClientResponse>> replace(
            @PathVariable UUID id, @Valid @RequestBody CreateClientRequest request) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(ApiResult.success(
                "อัปเดตข้อมูลลูกค้าสำเร็จ", clientService.replace(ownerId, id, request)));
    }

    @Operation(summary = "Update a client", description = "Update fields of a client belonging to the authenticated user")
    @ApiResponse(responseCode = "200", description = "Client updated", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "Invalid client data", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<ClientResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequest request) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(ApiResult.success(
                "อัปเดตข้อมูลลูกค้าสำเร็จ", clientService.update(ownerId, id, request)));
    }

    @Operation(summary = "Change client status", description = "Activate or deactivate a client by updating only its is_active flag")
    @ApiResponse(responseCode = "200", description = "Client status updated", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "400", description = "isActive is required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<ClientResponse>> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeClientStatusRequest request) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        return ResponseEntity.ok(ApiResult.success(
                "อัปเดตสถานะลูกค้าสำเร็จ", clientService.changeStatus(ownerId, id, request.getIsActive())));
    }

    /** Soft-delete: preserve the client and its active status while hiding it from Client APIs. */
    @Operation(summary = "Soft-delete a client", description = "Set deleted_at without changing is_active, while preserving client history")
    @ApiResponse(responseCode = "204", description = "Client soft-deleted", content = @Content)
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @ApiResponse(responseCode = "404", description = "Client not found", content = @Content(schema = @Schema(implementation = ApiResult.class)))
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        UUID ownerId = userService.getCurrentUserEntity().getId();
        clientService.softDelete(ownerId, id);
        return ResponseEntity.noContent().build();
    }
}
