package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import th.ac.kku.freelance_hub.dto.request.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.TimeEntrySummaryResponse;
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.service.TimeEntryQueryService;
import th.ac.kku.freelance_hub.service.UserService;

@Tag(
        name = "Time Entries",
        description = "Manage time entries belonging to the authenticated user"
)
@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final TimeEntryQueryService timeEntryQueryService;
    private final UserService userService;

    @Operation(summary = "Create a manual time entry")
    @ApiResponse(
            responseCode = "201",
            description = "Manual time entry created",
            content = @Content(
                    schema = @Schema(implementation = TimeEntryResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid time entry")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(
            responseCode = "404",
            description = "Project or task not found",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PostMapping
    public ResponseEntity<TimeEntryResponse> createManual(
            @Valid @RequestBody ManualTimeEntryRequest request
    ) {
        TimeEntryResponse response = timeEntryService.createManual(
                currentOwnerId(),
                request
        );
        return ResponseEntity
                .created(URI.create("/api/time-entries/" + response.getId()))
                .body(response);
    }

    @Operation(summary = "List time entries")
    @ApiResponse(responseCode = "200", description = "Page returned")
    @ApiResponse(responseCode = "400", description = "Invalid filters")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping
    public ResponseEntity<Page<TimeEntryResponse>> list(
            @Valid @ModelAttribute TimeEntryFilterRequest filter
    ) {
        return ResponseEntity.ok(
                timeEntryQueryService.list(currentOwnerId(), filter)
        );
    }

    @Operation(summary = "Summarize completed time entries")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @ApiResponse(responseCode = "400", description = "Invalid filters")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping("/summary")
    public ResponseEntity<TimeEntrySummaryResponse> summarize(
            @ModelAttribute TimeEntryFilterRequest filter
    ) {
        return ResponseEntity.ok(
                timeEntryQueryService.summarize(currentOwnerId(), filter)
        );
    }

    @Operation(summary = "Update an unlocked time entry")
    @ApiResponse(
            responseCode = "200",
            description = "Time entry updated",
            content = @Content(
                    schema = @Schema(implementation = TimeEntryResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid update")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    @ApiResponse(responseCode = "409", description = "Time entry is locked")
    @PatchMapping("/{id}")
    public ResponseEntity<TimeEntryResponse> update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTimeEntryRequest request
    ) {
        return ResponseEntity.ok(
                timeEntryService.update(currentOwnerId(), id, request)
        );
    }

    @Operation(summary = "Delete an unlocked completed time entry")
    @ApiResponse(
            responseCode = "204",
            description = "Time entry deleted",
            content = @Content
    )
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    @ApiResponse(
            responseCode = "409",
            description = "Time entry is locked or still running"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        timeEntryService.delete(currentOwnerId(), id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}
