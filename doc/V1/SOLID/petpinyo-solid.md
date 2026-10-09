# SOLID Analysis: Authentication และ User/Profile

**ผู้รับผิดชอบ:** เพชรภิญโญ ธนศิรินรากร (`petpinyo_673380073-7_02`)  
**ขอบเขต:** registration, login, JWT, refresh/logout, login throttling, current-user profile และ change password

## หลักฐานตาม SOLID

| Principle | หลักฐานในโค้ด | เหตุผล |
|---|---|---|
| SRP | `AuthController`, `UserController` | จัดการ HTTP contract และ cookie โดยส่ง business operation ไป service |
| SRP | `UserMapper` | แยก entity/DTO mapping และ PATCH mapping ออกจาก controller/service |
| SRP | `RefreshTokenService` | ดูแล issue, rotate, revoke, cleanup และ hash refresh token ภายในขอบเขต token lifecycle |
| SRP | `LoginAttemptLimiter` | แยกกฎ rate limit ต่อ email/IP ออกจาก authentication use case |
| OCP | `AuthService`, `AuthServiceImpl` | Controller พึ่ง interface; เปลี่ยน implementation หรือสร้าง test double ได้โดยไม่แก้ web layer |
| OCP | `SecurityConfig`, `PasswordEncoder` | เลือก implementation ของ password encoder ที่ configuration โดย use case พึ่ง abstraction |
| LSP | `AuthServiceImpl` | implement contract ของ `AuthService` ที่ `AuthController` ใช้ |
| LSP | `UserService`, `CurrentUserProvider` | `UserService` ใช้แทน provider abstraction ได้และคืน `UUID` ตาม contract |
| ISP | `AuthService` | มีเฉพาะ auth use cases ไม่บังคับ consumer ให้พึ่ง profile/client/project operations |
| ISP | `CurrentUserProvider` | Service อื่นที่ต้องใช้ owner ID พึ่ง interface ขนาดเล็ก method เดียว |
| DIP | `AuthServiceImpl` | รับ dependencies ผ่าน constructor และพึ่ง `UserRepository`, `PasswordEncoder`, `AuthenticationManager` และ service abstractions |
| DIP | `UserService` | รับ `UserRepository`, `PasswordEncoder`, `UserMapper` และ `RefreshTokenService` ผ่าน constructor |

## Layered Architecture และ DTO

เส้นทาง register/login:

```text
AuthController
  -> AuthService (interface)
  -> AuthServiceImpl
  -> UserRepository / RefreshTokenService / JwtTokenProvider / UserMapper
  -> User / UserProfile / RefreshToken
```

เส้นทาง profile/password:

```text
UserController
  -> UserService
  -> UserRepository / UserMapper / PasswordEncoder / RefreshTokenService
  -> User / UserProfile
```

Controller ไม่เรียก Repository โดยตรง และ API ใช้ `RegisterRequest`, `LoginRequest`, `UpdateUserProfileRequest`, `AuthResponse` และ `UserResponse` แทนการรับ/ส่ง Entity จึงไม่เปิดเผย `passwordHash` หรือ refresh-token hash

`UserProfileRepository` มีอยู่ใน codebase แต่เส้นทาง Auth/User นี้อ่านและบันทึก profile ผ่าน `UserRepository` และความสัมพันธ์ `User.profile`; จึงไม่แสดงเป็น dependency ของ `UserService` ใน diagram นี้.

## Business rules ที่แยกความรับผิดชอบ

- Email ถูก normalize ก่อนค้นหา/บันทึกใน register และ login: `AuthServiceImpl.register`, `AuthServiceImpl.login`
- Password ถูก hash ผ่าน `PasswordEncoder` ตอน register และเปลี่ยนรหัสผ่าน: `AuthServiceImpl.register`, `UserService.changePassword`
- Login failure ถูกนับและจำกัดตาม email/IP: `AuthServiceImpl.login`, `LoginAttemptLimiter`
- Refresh token ถูก rotate, คง family/expiry และตรวจ replay: `RefreshTokenService.rotate`
- การเปลี่ยนรหัสผ่านตรวจรหัสเดิมและ revoke refresh token ทั้งหมด: `UserService.changePassword`
- PATCH profile apply เฉพาะค่าที่ส่งมา: `UserMapper.updateProfile`

## ขอบเขตและข้อสังเกต

- `UserService` เป็น concrete service แต่ expose `CurrentUserProvider` interface สำหรับ consumer ที่ต้องการเพียง owner ID
- `AuthServiceImpl.login` ยังใช้ `RuntimeException` หาก authentication ผ่านแต่ค้น user ไม่พบ และ `UserService.getCurrentUserEmail` ใช้ `RuntimeException` เมื่อไม่มี authenticated user; ควรพิจารณาเปลี่ยนเป็น exception ที่สื่อความหมายเฉพาะเมื่อแก้โค้ด
- หลักฐานอ้างชื่อคลาสและ method แทนเลขบรรทัด เพื่อให้ยังตรวจตามได้เมื่อ source code เปลี่ยนบรรทัด

สรุปรวมของกลุ่มอยู่ที่ [doc/solid-analysis.md](../../solid-analysis.md)
