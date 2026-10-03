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
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.service.TimerService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.timeentry.StartTimerRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.CurrentTimerResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.StoppedTimerResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
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
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "400", description = "Invalid timer request")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Project or task not found")
    @ApiResponse(
            responseCode = "409",
            description = "A timer is already running",
            content = @Content(
                    schema = @Schema(implementation = ApiResult.class)
            )
    )
    @PostMapping("/start")
    public ResponseEntity<
            ApiResult<TimeEntryResponse>
    > start(
            @Valid @RequestBody StartTimerRequest request
    ) {
        TimeEntryResponse response = timerService.startTimer(
                currentOwnerId(),
                request
        );
        return ResponseEntity
                .created(URI.create("/api/time-entries/" + response.getId()))
                .body(ApiResult.success(
                        "เริ่มจับเวลาเรียบร้อยแล้ว",
                        response
                ));
    }

    @Operation(summary = "Get the current running timer")
    @ApiResponse(responseCode = "200", description = "Current timer returned", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping("/current")
    public ResponseEntity<
            ApiResult<CurrentTimerResponse>
    > current() {
        var currentTimer = timerService.getCurrentTimer(currentOwnerId());
        CurrentTimerResponse data = currentTimer
                .map(CurrentTimerResponse::active)
                .orElseGet(CurrentTimerResponse::inactive);
        String message = currentTimer.isPresent()
                ? "ดึงข้อมูลตัวจับเวลาที่กำลังทำงานเรียบร้อยแล้ว"
                : "ไม่มีตัวจับเวลาที่กำลังทำงาน";

        return ResponseEntity.ok(
                ApiResult.success(
                        message,
                        data
                )
        );
    }

    @Operation(summary = "Stop the current timer")
    @ApiResponse(
            responseCode = "200",
            description = "Timer stopped",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "No timer is running")
    @PostMapping("/stop")
    public ResponseEntity<
            ApiResult<StoppedTimerResponse>
    > stop() {
        TimeEntryResponse stoppedTimer = timerService.stopTimer(
                currentOwnerId()
        );
        return ResponseEntity.ok(
                ApiResult.success(
                        "หยุดจับเวลาเรียบร้อยแล้ว",
                        StoppedTimerResponse.from(stoppedTimer)
                )
        );
    }

    @Operation(summary = "Cancel the current timer")
    @ApiResponse(
            responseCode = "200",
            description = "Timer cancelled",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "No timer is running")
    @DeleteMapping("/current")
    public ResponseEntity<
            ApiResult<Void>
    > cancel() {
        timerService.cancelTimer(currentOwnerId());
        return ResponseEntity.ok(
                ApiResult
                        .<Void>success(
                                "ยกเลิกการจับเวลาเรียบร้อยแล้ว",
                                null
                        )
        );
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}
