package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import th.ac.kku.freelance_hub.dto.request.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.TimeEntryResponse;
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.TimerService;
import th.ac.kku.freelance_hub.service.UserService;

@Tag(name = "Timer", description = "Control the authenticated user's timer")
@RestController
@RequestMapping("/api/timer")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TimerController {

    private final TimerService timerService;
    private final UserService userService;

    @Operation(summary = "Start a timer")
    @ApiResponse(
            responseCode = "201",
            description = "Timer started",
            content = @Content(
                    schema = @Schema(implementation = TimeEntryResponse.class)
            )
    )
    @ApiResponse(responseCode = "400", description = "Invalid timer request")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found")
    @ApiResponse(
            responseCode = "409",
            description = "A timer is already running",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PostMapping("/start")
    public ResponseEntity<TimeEntryResponse> start(
            @Valid @RequestBody StartTimerRequest request
    ) {
        TimeEntryResponse response = timerService.startTimer(
                currentOwnerId(),
                request
        );
        return ResponseEntity
                .created(URI.create("/api/time-entries/" + response.getId()))
                .body(response);
    }

    @Operation(summary = "Get the current running timer")
    @ApiResponse(responseCode = "200", description = "Current timer returned")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(
            responseCode = "404",
            description = "No timer is running",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @GetMapping("/current")
    public ResponseEntity<TimeEntryResponse> current() {
        return ResponseEntity.ok(
                timerService.getCurrentTimer(currentOwnerId())
        );
    }

    @Operation(summary = "Stop the current timer")
    @ApiResponse(responseCode = "200", description = "Timer stopped")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "No timer is running")
    @PostMapping("/stop")
    public ResponseEntity<TimeEntryResponse> stop() {
        return ResponseEntity.ok(
                timerService.stopTimer(currentOwnerId())
        );
    }

    @Operation(summary = "Cancel the current timer")
    @ApiResponse(
            responseCode = "204",
            description = "Timer cancelled",
            content = @Content
    )
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "No timer is running")
    @DeleteMapping("/current")
    public ResponseEntity<Void> cancel() {
        timerService.cancelTimer(currentOwnerId());
        return ResponseEntity.noContent().build();
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}
