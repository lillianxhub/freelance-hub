package th.ac.kku.freelance_hub.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import th.ac.kku.freelance_hub.common.response.PaginationMeta;
import th.ac.kku.freelance_hub.service.TimeEntryService;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.timeentry.ManualTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.TimeEntryFilterRequest;
import th.ac.kku.freelance_hub.dto.request.timeentry.UpdateTimeEntryRequest;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryDetailResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryListItemResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntryResponse;
import th.ac.kku.freelance_hub.dto.response.timeentry.TimeEntrySummaryResponse;
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
    private final UserService userService;

    @Operation(summary = "Create a manual time entry")
    @ApiResponse(
            responseCode = "201",
            description = "Manual time entry created",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "400", description = "Invalid time entry")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(
            responseCode = "404",
            description = "Project or task not found",
            content = @Content(
                    schema = @Schema(implementation = ApiResult.class)
            )
    )
    @PostMapping
    public ResponseEntity<
            ApiResult<TimeEntryDetailResponse>
    > createManual(
            @Valid @RequestBody ManualTimeEntryRequest request
    ) {
        TimeEntryResponse response = timeEntryService.createManual(
                currentOwnerId(),
                request
        );
        return ResponseEntity
                .created(URI.create("/api/time-entries/" + response.getId()))
                .body(ApiResult.success(
                        "เพิ่มรายการเวลาเรียบร้อยแล้ว",
                        TimeEntryDetailResponse.from(response)
                ));
    }

    @Operation(summary = "List time entries")
    @ApiResponse(
            responseCode = "200",
            description = "Page returned",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "400", description = "Invalid filters")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping
    public ResponseEntity<
            ApiResult<
                    List<TimeEntryListItemResponse>
            >
    > list(
            @ParameterObject @Valid @ModelAttribute TimeEntryFilterRequest filter
    ) {
        Page<TimeEntryResponse> page = timeEntryService.list(
                currentOwnerId(),
                filter
        );
        List<TimeEntryListItemResponse> data = page.getContent().stream()
                .map(TimeEntryListItemResponse::from)
                .toList();
        PaginationMeta meta = PaginationMeta.builder()
                .page(page.getNumber() + 1)
                .limit(page.getSize())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงข้อมูลรายการเวลาเรียบร้อยแล้ว",
                        data,
                        meta
                )
        );
    }

    @Operation(summary = "Get time entry details")
    @ApiResponse(
            responseCode = "200",
            description = "Time entry returned",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    @GetMapping("/{id}")
    public ResponseEntity<
            ApiResult<TimeEntryDetailResponse>
    > getById(@PathVariable("id") UUID id) {
        TimeEntryResponse response = timeEntryService.getById(
                currentOwnerId(),
                id
        );

        return ResponseEntity.ok(
                ApiResult.success(
                        "ดึงข้อมูลรายการเวลาเรียบร้อยแล้ว",
                        TimeEntryDetailResponse.from(response)
                )
        );
    }

    @Operation(summary = "Summarize completed time entries")
    @ApiResponse(responseCode = "200", description = "Summary returned", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid filters")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping("/summary")
    public ResponseEntity<ApiResult<TimeEntrySummaryResponse>> summarize(
            @ParameterObject @ModelAttribute TimeEntryFilterRequest filter
    ) {
        return ResponseEntity.ok(
                ApiResult.success(
                        "สรุปรายการเวลาเรียบร้อยแล้ว",
                        timeEntryService.summarize(currentOwnerId(), filter)
                )
        );
    }

    @Operation(summary = "Update an unlocked time entry")
    @ApiResponse(
            responseCode = "200",
            description = "Time entry updated",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "400", description = "Invalid update")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    @ApiResponse(responseCode = "409", description = "Time entry is locked")
    @PutMapping("/{id}")
    public ResponseEntity<
            ApiResult<TimeEntryDetailResponse>
    > update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTimeEntryRequest request
    ) {
        TimeEntryResponse response = timeEntryService.update(
                currentOwnerId(),
                id,
                request
        );
        return ResponseEntity.ok(
                ApiResult.success(
                        "แก้ไขรายการเวลาเรียบร้อยแล้ว",
                        TimeEntryDetailResponse.from(response)
                )
        );
    }

    @Operation(summary = "Delete an unlocked completed time entry")
    @ApiResponse(
            responseCode = "200",
            description = "Time entry deleted",
            useReturnTypeSchema = true
    )
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "404", description = "Time entry not found")
    @ApiResponse(
            responseCode = "409",
            description = "Time entry is locked or still running"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<
            ApiResult<Void>
    > delete(@PathVariable("id") UUID id) {
        timeEntryService.delete(currentOwnerId(), id);
        return ResponseEntity.ok(
                ApiResult.success(
                        "ลบรายการเวลาเรียบร้อยแล้ว",
                        null
                )
        );
    }

    private UUID currentOwnerId() {
        return userService.getCurrentUserEntity().getId();
    }
}
