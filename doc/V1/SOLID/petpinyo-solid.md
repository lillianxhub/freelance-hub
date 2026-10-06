# SOLID Analysis: Authentication และ User/Profile

**ผู้รับผิดชอบ:** เพชรภิญโญ ธนศิรินรากร (`petpinyo_673380073-7_02`)  
**ขอบเขต:** registration, login, JWT, refresh/logout, login throttling, current-user profile และ change password

## หลักฐานตาม SOLID

| Principle | หลักฐานในโค้ด | เหตุผล |
|---|---|---|
| SRP | `AuthController.java:32–109` | จัดการ HTTP contract และ cookie เท่านั้น แล้วส่ง business operation ไป `AuthService` |
| SRP | `UserMapper.java:14–93` | แยก entity/DTO mapping และ PATCH mapping ออกจาก controller/service |
| SRP | `RefreshTokenService.java:22–103` | ดูแล issue, rotate, revoke, cleanup และ hash refresh token ภายในขอบเขต token lifecycle |
| SRP | `LoginAttemptLimiter.java:11–63` | แยกกฎ rate limit ต่อ email/IP ออกจาก authentication use case |
| OCP | `AuthService.java:7–15`, `AuthServiceImpl.java:29–40` | Controller ใช้ interface; เปลี่ยน implementation หรือสร้าง test double ได้โดยไม่แก้ web layer |
| OCP | `SecurityConfig.java:63–78` | เปลี่ยน provider/encoder ได้ที่ configuration โดยไม่แก้ use case |
| LSP | `AuthServiceImpl.java:31–103` | implement ทุก method ของ `AuthService` โดยรักษา contract ที่ controller ใช้ |
| LSP | `CurrentUserProvider.java:5–8`, `UserService.java:25–30,81–88` | `UserService` ใช้แทน provider abstraction ได้และคืน `UUID` ตาม contract |
| ISP | `AuthService.java:7–15` | มีเฉพาะ auth use cases ไม่บังคับ consumer ให้พึ่ง profile/client/project operations |
| ISP | `CurrentUserProvider.java:5–8` | Service อื่นที่ต้องใช้ owner ID พึ่ง interface ขนาดเล็ก method เดียว |
| DIP | `AuthServiceImpl.java:31–40` | dependency ถูก constructor-inject และ service พึ่ง `UserRepository`, `PasswordEncoder`, `AuthenticationManager` abstractions |
| DIP | `UserService.java:25–30` | service ไม่สร้าง repository/encoder/mapper/token service เอง |

## Layered Architecture และ DTO

เส้นทาง register/login:

```text
AuthController
  -> AuthService (interface)
  -> AuthServiceImpl
  -> UserRepository / RefreshTokenService
  -> User / UserProfile / RefreshToken
```

เส้นทาง profile/password:

```text
UserController
  -> UserService
  -> UserRepository
  -> UserMapper
  -> User / UserProfile
```

Controller ไม่เรียก Repository โดยตรง และ API ใช้ `RegisterRequest`, `LoginRequest`, `UpdateUserProfileRequest`, `AuthResponse` และ `UserResponse` แทนการรับ/ส่ง Entity จึงไม่เปิดเผย `passwordHash` หรือ refresh-token hash

## Business rules ที่แยกความรับผิดชอบ

- Email ถูก normalize ก่อนค้นหา/บันทึก: `AuthServiceImpl.java:44–47,64–67`
- Password ถูก hash ผ่าน `PasswordEncoder`: `AuthServiceImpl.java:50–53`
- Login failure ถูกนับและคืน rate limit: `AuthServiceImpl.java:66–74`
- Refresh token ถูก rotate และตรวจ replay: `RefreshTokenService.java:41–62`
- การเปลี่ยนรหัสผ่านตรวจรหัสเดิมและ revoke refresh token ทั้งหมด: `UserService.java:55–68`
- PATCH profile apply เฉพาะค่าที่ส่งมา: `UserMapper.java:58–89`

## ขอบเขตและข้อสังเกต

- `UserService` เป็น concrete service แต่ expose `CurrentUserProvider` interface สำหรับ consumer ที่ต้องการเพียง owner ID
- `AuthServiceImpl.java:76–77` ยังใช้ `RuntimeException` ในกรณีที่ authentication ผ่านแต่ค้น user ไม่พบ ควรเปลี่ยนเป็น domain exception หากมีการปรับโค้ดรอบถัดไป
- เลขบรรทัดต้องตรวจอีกครั้งหาก source code เปลี่ยนก่อนส่ง

สรุปรวมของกลุ่มอยู่ที่ [doc/solid-analysis.md](../../solid-analysis.md)
