# SOLID Analysis: Authentication และ User/Profile

**เจ้าของ feature:** `petpinyo_673380073-7_02`  
**ขอบเขต:** register, login, logout, JWT authentication, user profile และ role authorization

เอกสารนี้อ้างอิง implementation ปัจจุบัน เลขบรรทัดต้องตรวจซ้ำหลัง code freeze

| Principle | ไฟล์/คลาสและบรรทัด | เหตุผลที่ใช้ |
|---|---|---|
| Single Responsibility | `controller/AuthController.java:22-105` | รับ HTTP request, validate DTO, เรียก service และคืน status เท่านั้น |
| Single Responsibility | `service/impl/AuthServiceImpl.java:22-87` | ประสาน use case register/login/logout และสร้าง auth response |
| Single Responsibility | `mapper/UserMapper.java:12-58` | แปลง `User`/`UserProfile` กับ DTO ไม่ปน persistence หรือ HTTP |
| Single Responsibility | `security/JwtTokenProvider.java:17-95` | สร้าง อ่าน และ validate JWT แยกจาก auth service |
| Open/Closed | `service/AuthService.java:7-15`, `service/impl/AuthServiceImpl.java:22-24` | Controller ขึ้นกับ service contract; เพิ่ม implementation หรือ test double ได้โดยไม่แก้ controller |
| Open/Closed | `config/SecurityConfig.java:66-80` | ใช้ `AuthenticationProvider` และ `PasswordEncoder` abstraction ทำให้เปลี่ยน provider/encoder ได้จาก configuration |
| Liskov Substitution | `AuthService.java:8-15`, `AuthServiceImpl.java:24-81` | `AuthServiceImpl` implement operation ครบตาม contract และถูกใช้ผ่าน `AuthService` ได้ |
| Interface Segregation | `AuthService.java:8-15` | interface มีเฉพาะ register/login/logout ไม่รวม operation ของ Client หรือ Project |
| Interface Segregation | `repository/UserRepository.java:13-25`, `repository/RevokedTokenRepository.java:9-14` | แยก persistence contract ของ User กับ revoked token ตามหน้าที่ |
| Dependency Inversion | `service/impl/AuthServiceImpl.java:22-31` | service รับ `UserRepository`, `PasswordEncoder`, `AuthenticationManager`, `JwtTokenProvider`, `UserMapper` และ `RevokedTokenService` ผ่าน constructor |
| Dependency Inversion | `config/SecurityConfig.java:31-35,66-80` | configuration เป็นจุดประกอบ concrete bean; business logic ไม่สร้าง dependency เอง |

## Evidence จาก flow

- Registration ตรวจ email ซ้ำ, hash password, สร้าง profile และบันทึกผ่าน repository: `AuthServiceImpl.java:33-49`
- Login ใช้ `AuthenticationManager` ตรวจ credentials ก่อนสร้าง token: `AuthServiceImpl.java:51-64`
- Logout ตรวจ token subject และ revoke JTI: `AuthServiceImpl.java:66-81`
- User profile ถูก map โดยไม่ส่ง `passwordHash` ออก API: `UserMapper.java:18-43`, `UserResponse.java:20-40`
- JWT filter ตรวจ token/revocation แล้วใส่ principal ใน SecurityContext: `JwtAuthenticationFilter.java:30-66`

## ข้อสังเกตสำหรับปรับปรุง

1. `UserService` ยังเป็น concrete class (`service/UserService.java:18-20`) จึงมีหลักฐาน DIP/LSP น้อยกว่า `AuthService` หาก scope อนุญาตควรแยก interface
2. `AuthServiceImpl` ใช้ `RuntimeException` กรณี user ไม่พบหลัง authentication (`AuthServiceImpl.java:61-63,74-75`) ควรใช้ domain exception ที่ handler รองรับ
3. ต้องตรวจ log ใน `JwtAuthenticationFilter.java:62-64` ไม่ให้มี token หรือข้อมูลลับ
4. ก่อนส่งต้องรัน test และอัปเดตเลขบรรทัดในตารางให้ตรงกับ commit สุดท้าย
