package th.ac.kku.freelance_hub.service;
import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
/** Authentication use cases exposed to the web layer. */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthSessionResult login(LoginRequest request);

    AuthSessionResult refresh(String refreshToken);

    void logout(String refreshToken);
}
