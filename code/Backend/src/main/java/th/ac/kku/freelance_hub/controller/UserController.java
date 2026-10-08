package th.ac.kku.freelance_hub.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.service.UserService;
import th.ac.kku.freelance_hub.dto.request.auth.ChangePasswordRequest;
import th.ac.kku.freelance_hub.dto.request.user.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
/**
 * REST Controller for user endpoints
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get current authenticated user profile
     * GET /api/users/me
     */
    @Tag(name = "Authentication")
    @ApiResponse(responseCode = "200", description = "Current user returned", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserResponse>> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResult.success("ดึงข้อมูลผู้ใช้สำเร็จ", response));
    }

    /**
     * Update the authenticated user's personal information and address.
     * PATCH /api/users/me
     */
    @Tag(name = "Authentication")
    @ApiResponse(responseCode = "200", description = "User profile updated", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<UserResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(ApiResult.success(
                "อัปเดตข้อมูลผู้ใช้สำเร็จ", userService.updateCurrentUser(request)));
    }

    /**
     * Change the authenticated user's password using the old and new values.
     * PATCH /api/users/me/password
     */
    @Tag(name = "Authentication")
    @ApiResponse(responseCode = "200", description = "Password changed", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResult.success("เปลี่ยนรหัสผ่านสำเร็จ", null));
    }

}
