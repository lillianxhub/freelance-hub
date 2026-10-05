package th.ac.kku.freelance_hub.service;
import th.ac.kku.freelance_hub.dto.request.auth.LoginRequest;
import th.ac.kku.freelance_hub.dto.request.auth.RegisterRequest;
import th.ac.kku.freelance_hub.dto.response.auth.AuthResponse;
import th.ac.kku.freelance_hub.dto.response.user.UserResponse;
/** Authentication use cases exposed to the web layer. */
public interface AuthService {

    AuthSessionResult register(RegisterRequest request);

    AuthSessionResult login(LoginRequest request, String remoteAddress);

    AuthSessionResult refresh(String refreshToken);

    void logout(String refreshToken);
}
