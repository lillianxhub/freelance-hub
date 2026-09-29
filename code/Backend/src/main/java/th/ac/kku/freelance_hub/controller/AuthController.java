package th.ac.kku.freelance_hub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import th.ac.kku.freelance_hub.dto.request.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.AuthResponse;
import th.ac.kku.freelance_hub.exception.ErrorResponse;
import th.ac.kku.freelance_hub.service.AuthService;
import th.ac.kku.freelance_hub.service.AuthSessionResult;
import th.ac.kku.freelance_hub.security.RefreshTokenCookie;

import java.util.Arrays;

@Tag(name = "Authentication", description = "Authentication and registration endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final RefreshTokenCookie refreshTokenCookie;

        @Value("${app.cors.allowed-origins:http://localhost:5173}")
        private String allowedOrigins;

        private void verifyOrigin(String origin) {
                if (origin != null && Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim).noneMatch(origin::equals)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origin not allowed");
                }
        }

        @Operation(summary = "Register new user", description = "Create a new user account with email and password")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @SecurityRequirements
        @PostMapping("/register")
        public ResponseEntity<th.ac.kku.freelance_hub.dto.response.ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
                AuthResponse response = authService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(th.ac.kku.freelance_hub.dto.response.ApiResponse.success("Registration successful", response));
        }

        @Operation(summary = "Login user", description = "Authenticate user with email and password")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
        @SecurityRequirements
        @PostMapping("/login")
        public ResponseEntity<th.ac.kku.freelance_hub.dto.response.ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
                AuthSessionResult result = authService.login(request);
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.set(result.refreshToken(), result.refreshExpiresAt()))
                        .body(th.ac.kku.freelance_hub.dto.response.ApiResponse.success("Login successful", result.response()));
        }

        @Operation(summary = "Rotate refresh token", description = "Issue a new access token and rotate the HttpOnly refresh cookie")
        @SecurityRequirements
        @PostMapping("/refresh")
        public ResponseEntity<th.ac.kku.freelance_hub.dto.response.ApiResponse<AuthResponse>> refresh(
                        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
                        @RequestHeader(name = "Origin", required = false) String origin) {
                verifyOrigin(origin);
                AuthSessionResult result = authService.refresh(refreshToken);
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.set(result.refreshToken(), result.refreshExpiresAt()))
                        .body(th.ac.kku.freelance_hub.dto.response.ApiResponse.success("Token refreshed", result.response()));
        }

        @Operation(summary = "Logout user", description = "Revoke the current refresh-token family and clear its cookie; access tokens remain valid until expiry")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Logout successful"),
                        @ApiResponse(responseCode = "403", description = "Origin not allowed")
        })
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
                        @RequestHeader(name = "Origin", required = false) String origin) {
                verifyOrigin(origin);
                authService.logout(refreshToken);
                return ResponseEntity.noContent()
                        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                        .build();
        }
}
