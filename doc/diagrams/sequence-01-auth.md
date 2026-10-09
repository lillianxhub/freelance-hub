# Sequence 01: Login และ Protected Request

ที่มา: [Petpinyo](../V1/USECASE/petpinyo-usecase.md) ตรวจชื่อ method/response กับ AuthController และ JwtTokenProvider ณ `cb8002d` Diagram ย่อขั้นตรวจ credentials/JWT; รายละเอียด refresh-token rotation และ error flows ดู [Use Cases](../use-case-description.md)

```mermaid
sequenceDiagram
    actor User
    participant C as AuthController
    participant S as AuthServiceImpl
    participant A as AuthenticationManager
    participant D as CustomUserDetailsService
    participant J as JwtTokenProvider
    participant F as JwtAuthenticationFilter
    participant U as UserController

    User->>C: POST /api/auth/login
    C->>S: login(LoginRequest)
    S->>A: authenticate(email, password)
    A->>D: loadUserByUsername(email)
    D-->>A: UserDetails + authority
    A-->>S: authenticated
    S->>J: generateToken(email)
    J-->>S: JWT
    S-->>C: AuthSessionResult
    C-->>User: 200 ApiResult + HttpOnly refresh cookie
    User->>F: GET /api/users/me + Bearer JWT
    F->>J: validateToken / getEmailFromToken
    F->>D: loadUserByUsername(email)
    F->>U: continue with SecurityContext
    U-->>User: 200 ApiResult UserResponse
```
