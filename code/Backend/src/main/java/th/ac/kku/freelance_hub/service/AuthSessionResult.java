package th.ac.kku.freelance_hub.service;

import java.time.Instant;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
/** Access-token response plus a refresh credential that must only be sent as an HttpOnly cookie. */
public record AuthSessionResult(AuthResponse response, String refreshToken, Instant refreshExpiresAt) { }
