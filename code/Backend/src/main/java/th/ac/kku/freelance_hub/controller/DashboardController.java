package th.ac.kku.freelance_hub.controller;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.service.DashboardService;
import th.ac.kku.freelance_hub.service.CurrentUserProvider;
import th.ac.kku.freelance_hub.dto.request.dashboard.DashboardActivityPeriod;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardActivityResponse;
import th.ac.kku.freelance_hub.dto.response.dashboard.DashboardResponse;
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider userService;

    @GetMapping
    public ResponseEntity<ApiResult<DashboardResponse>> getDashboard() {
        UUID ownerId = userService.currentUserId();
        DashboardResponse data = dashboardService.getDashboard(ownerId);

        return ResponseEntity.ok(
                ApiResult.success("ดึงข้อมูล Dashboard สำเร็จ", data)
        );
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResult<DashboardActivityResponse>> getActivity(
            @RequestParam(defaultValue = "WEEK") DashboardActivityPeriod period
    ) {
        UUID ownerId = userService.currentUserId();
        DashboardActivityResponse data = dashboardService.getActivity(ownerId, period);
        return ResponseEntity.ok(ApiResult.success("ดึงข้อมูลกราฟ Dashboard สำเร็จ", data));
    }
}
