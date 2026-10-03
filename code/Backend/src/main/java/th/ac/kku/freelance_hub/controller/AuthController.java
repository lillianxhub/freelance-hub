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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import th.ac.kku.freelance_hub.common.response.ApiResult;
import th.ac.kku.freelance_hub.service.AuthService;
import th.ac.kku.freelance_hub.service.AuthSessionResult;
import th.ac.kku.freelance_hub.security.RefreshTokenCookie;
import th.ac.kku.freelance_hub.security.EmailNormalizer;
import th.ac.kku.freelance_hub.security.LoginAttemptLimiter;
import th.ac.kku.freelance_hub.exception.LoginRateLimitedException;

import java.net.URI;
import java.util.Arrays;
import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
@Tag(name = "Authentication", description = "Authentication and registration endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;
        private final RefreshTokenCookie refreshTokenCookie;
        private final LoginAttemptLimiter loginAttemptLimiter;

        @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:8080}")
        private String allowedOrigins;

        private void verifyOrigin(String origin, String referer) {
                String source = origin;
                if (source == null && referer != null) {
                        try {
                                URI uri = URI.create(referer);
                                if (uri.getScheme() != null && uri.getHost() != null && uri.getUserInfo() == null) {
                                        source = uri.getScheme() + "://" + uri.getHost()
                                                        + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
                                }
                        } catch (IllegalArgumentException ignored) {
                                // Malformed Referer is not a trusted origin.
                        }
                }
                final String trustedSource = source;
                if (trustedSource == null || Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim).noneMatch(trustedSource::equals)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Origin not allowed");
                }
        }

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
                AuthResponse response = authService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResult.success("สมัครสมาชิกสำเร็จ",
                                                response));
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
                String email = EmailNormalizer.normalize(request.getEmail());
                String address = servletRequest.getRemoteAddr();
                long retryAfter = loginAttemptLimiter.retryAfter(email, address);
                if (retryAfter > 0) throw new LoginRateLimitedException(retryAfter);
                AuthSessionResult result;
                try {
                        result = authService.login(request);
                } catch (BadCredentialsException ex) {
                        loginAttemptLimiter.recordFailure(email, address);
                        throw ex;
                }
                loginAttemptLimiter.recordSuccess(email);
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
                        @ApiResponse(responseCode = "403", description = "Origin not allowed")
        })
        @SecurityRequirements
        @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ApiResult<AuthResponse>> refresh(
                        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
                        @RequestHeader(name = "Origin", required = false) String origin,
                        @RequestHeader(name = "Referer", required = false) String referer) {
                verifyOrigin(origin, referer);
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
        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
                        @RequestHeader(name = "Origin", required = false) String origin,
                        @RequestHeader(name = "Referer", required = false) String referer) {
                verifyOrigin(origin, referer);
                authService.logout(refreshToken);
                return ResponseEntity.noContent()
                                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear())
                                .build();
        }
}
