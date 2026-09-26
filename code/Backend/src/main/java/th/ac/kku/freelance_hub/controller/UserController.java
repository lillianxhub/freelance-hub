package th.ac.kku.freelance_hub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import th.ac.kku.freelance_hub.dto.request.ChangePasswordRequest;
import th.ac.kku.freelance_hub.dto.request.UpdateUserProfileRequest;
import th.ac.kku.freelance_hub.dto.response.UserResponse;
import th.ac.kku.freelance_hub.service.UserService;
import java.util.UUID;

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
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    /**
     * Update the authenticated user's personal information and address.
     * PATCH /api/users/me
     */
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(request));
    }

    /**
     * Change the authenticated user's password using the old and new values.
     * PATCH /api/users/me/password
     */
    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user by ID (admin only for now)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}
