package th.ac.kku.freelance_hub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.service.AuthService;
import th.ac.kku.freelance_hub.service.AuthSessionResult;
import th.ac.kku.freelance_hub.security.RefreshTokenCookie;
import th.ac.kku.freelance_hub.security.TrustedOriginValidator;

import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final RefreshTokenCookie refreshTokenCookie;
        private final TrustedOriginValidator originValidator;

        @Operation(summary = "Register new user", description = "Create a new user account with email and password")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User registered successfully", useReturnTypeSchema = true),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ApiResult.class))),
                        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ApiResult.class)))
        })
        @SecurityRequirements
        @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ApiResult<AuthResponse>> register(
                        @Valid @RequestBody RegisterRequest request) {
                AuthSessionResult result = authService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .header(HttpHeaders.SET_COOKIE,
                                                refreshTokenCookie.set(result.refreshToken(), result.refreshExpiresAt()))
                                .body(ApiResult.success("สมัครสมาชิกสำเร็จ", result.response()));
        }

        @Operation(summary = "Login user", description = "Authenticate user with email and password")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful", useReturnTypeSchema = true),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ApiResult.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiResult.class))),
                        @ApiResponse(responseCode = "429", description = "Login rate limited; check Retry-After", content = @Content(schema = @Schema(implementation = ApiResult.class)))
        })
        @SecurityRequirements
        @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ApiResult<AuthResponse>> login(
                        @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
                AuthSessionResult result = authService.login(request, servletRequest.getRemoteAddr());
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE,
                                                refreshTokenCookie.set(result.refreshToken(),
                                                                result.refreshExpiresAt()))
                                .body(ApiResult.success("เข้าสู่ระบบสำเร็จ",
                                                result.response()));
        }

        @Operation(summary = "Rotate refresh token", description = "Issue a new access token and rotate the HttpOnly refresh cookie")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Token refreshed", useReturnTypeSchema = true),
                        @ApiResponse(responseCode = "401", description = "Refresh token is missing, invalid, expired, or revoked", content = @Content(schema = @Schema(implementation = ApiResult.class))),
                        @ApiResponse(responseCode = "403", description = "Origin not allowed")
        })
        @SecurityRequirements
        @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ApiResult<AuthResponse>> refresh(
                        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
                        HttpServletRequest servletRequest) {
                originValidator.verify(servletRequest);
                AuthSessionResult result = authService.refresh(refreshToken);
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE,
                                                refreshTokenCookie.set(result.refreshToken(),
                                                                result.refreshExpiresAt()))
                                .body(ApiResult.success("Token refreshed",
                                                result.response()));
        }

        @Operation(summary = "Logout user", description = "Revoke the current refresh-token family and clear its cookie; access tokens remain valid until expiry")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Logout successful"),
                        @ApiResponse(responseCode = "403", description = "Origin not allowed")
        })
        @SecurityRequirements
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
                        HttpServletRequest servletRequest) {
                originValidator.verify(servletRequest);
                authService.logout(refreshToken);
                return ResponseEntity.noContent()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                                .build();
        }
}
