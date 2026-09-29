package th.ac.kku.freelance_hub.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import th.ac.kku.freelance_hub.dto.request.ChangePasswordRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.UserResponse;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.service.UserService;

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
    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResult.success("เปลี่ยนรหัสผ่านสำเร็จ", null));
    }

    /*
     * Reserved for a future admin user-management feature.
     * GET /api/users/{id}
     *
     * @GetMapping("/{id}")
     *
     * @PreAuthorize("hasRole('ADMIN')")
     * public ResponseEntity<ApiResult<UserResponse>> getUserById(@PathVariable
     * UUID id) {
     * UserResponse response = userService.getUserById(id);
     * return ResponseEntity.ok(ApiResult.success("User retrieved", response));
     * }
     */
}
