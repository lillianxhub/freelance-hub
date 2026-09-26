# SOLID Analysis: Authentication และ User/Profile

เอกสารนี้วิเคราะห์จาก implementation ปัจจุบัน ไม่ใช่ข้อเสนอของโค้ดในอนาคต
เลขบรรทัดอ้างอิงจากไฟล์ ณ วันที่จัดทำ draft และต้องตรวจซ้ำก่อนส่งจริง

## Summary

| Principle | หลักฐานจาก implementation | ไฟล์และบรรทัด | สรุป |
|---|---|---|---|
| Single Responsibility | Controller รับ HTTP, service ทำ use case, repository ทำ data access, mapper แปลง DTO/entity, security component จัดการ JWT/authentication | `AuthController.java:22-105`, `AuthServiceImpl.java:22-87`, `UserMapper.java:12-58`, `UserRepository.java:13-25`, `JwtTokenProvider.java:17-95` | แต่ละ class มีหน้าที่หลักแยกกัน ทำให้ทดสอบและเปลี่ยนส่วนย่อยได้ง่าย |
| Open/Closed | `AuthService` เป็น contract และ `AuthServiceImpl` เป็น implementation; `UserDetailsService`/`AuthenticationProvider` ใช้ abstraction ของ Spring Security | `AuthService.java:7-15`, `AuthServiceImpl.java:24`, `CustomUserDetailsService.java:22`, `SecurityConfig.java:66-71` | เพิ่ม implementation หรือเปลี่ยนรายละเอียดภายในโดยไม่ให้ controller รู้รายละเอียด |
| Liskov Substitution | Controller เรียก `AuthService` ผ่าน interface; `AuthServiceImpl` ให้ผลลัพธ์ตาม contract `register/login/logout` | `AuthController.java:28`, `AuthService.java:8-15`, `AuthServiceImpl.java:24-81` | implementation สามารถถูกใช้แทน service contract ได้โดยไม่เปลี่ยน caller |
| Interface Segregation | `AuthService` มีเฉพาะ operation ของ auth; `UserRepository` มี query ที่เกี่ยวกับ User; `RevokedTokenRepository` แยกจาก UserRepository | `AuthService.java:8-15`, `UserRepository.java:14-24`, `RevokedTokenRepository.java:9-14` | client ไม่ต้องพึ่ง method ของ feature ที่ไม่เกี่ยวข้อง |
| Dependency Inversion | `AuthServiceImpl` พึ่ง `AuthService`, `UserRepository`, `PasswordEncoder`, `AuthenticationManager`, `UserMapper`, `RevokedTokenService`; ทุก dependency inject ผ่าน constructor | `AuthServiceImpl.java:22-31`, `SecurityConfig.java:31-35`, `UserService.java:18-23` | business service ไม่สร้าง dependency เอง และ framework ประกอบ object graph ให้ |

## รายละเอียดตามหลัก

### Single Responsibility Principle (SRP)

- `AuthController` รับ request, เรียก service และกำหนด HTTP status เท่านั้น
  (`AuthController.java:52-105`)
- `AuthServiceImpl` ประสาน registration/login/logout และสร้าง auth response
  (`AuthServiceImpl.java:33-87`)
- `UserMapper` รับผิดชอบ mapping `User` + `UserProfile` ไป `UserResponse` และ
  สร้าง profile จาก registration request (`UserMapper.java:18-58`)
- `JwtTokenProvider` แยกการสร้าง/อ่าน/validate JWT ออกจาก auth service
  (`JwtTokenProvider.java:29-95`)

### Open/Closed Principle (OCP)

จุดขยายหลักอยู่ที่ contract ของ service และ Spring Security abstraction:
`AuthController` พึ่ง `AuthService` (`AuthController.java:28`), ขณะที่
`AuthServiceImpl` implement contract (`AuthServiceImpl.java:24`) หากเพิ่ม
implementation เช่น remote authentication หรือ test double ไม่จำเป็นต้องแก้
controller

### Liskov Substitution Principle (LSP)

`AuthServiceImpl` implement method ครบตาม `AuthService` และคืน `AuthResponse`/
`void` ตาม contract (`AuthService.java:10-14`, `AuthServiceImpl.java:33-81`)
จึงสามารถใช้แทน `AuthService` ใน controller หรือ test ได้

### Interface Segregation Principle (ISP)

การแยก `AuthService`, `UserRepository` และ `RevokedTokenRepository` ทำให้แต่ละ
consumer รับเฉพาะความสามารถที่ใช้จริง (`AuthService.java:8-15`,
`UserRepository.java:14-24`, `RevokedTokenRepository.java:9-14`)

### Dependency Inversion Principle (DIP)

`AuthServiceImpl` ใช้ constructor injection (`AuthServiceImpl.java:22-31`) และ
รับ abstraction เช่น `PasswordEncoder`, `AuthenticationManager` แทนการผูกกับ
algorithm หรือ authentication implementation โดยตรง ส่วน `SecurityConfig`
เป็นจุดประกอบ concrete bean (`SecurityConfig.java:66-80`)

## ประเด็นที่ควรตรวจ/ปรับปรุงก่อนส่ง

รายการนี้เป็น observation จากโค้ดจริง ไม่ใช่การอ้างว่า implementation ผ่านทุกข้อ
สมบูรณ์แล้ว:

1. `UserService` เป็น concrete class ไม่ใช่ interface (`UserService.java:18-20`)
   จึงยังแสดง DIP/LSP ได้น้อยกว่า `AuthService`; ถ้าต้องการหลักฐานที่แข็งแรง
   ควรแยก `UserService` interface ในงาน refactor ที่มี scope ชัดเจน
2. `AuthServiceImpl` ใช้ `RuntimeException` ในกรณี user ไม่พบหลัง authentication
   (`AuthServiceImpl.java:61-63`, `74-75`) ควรใช้ exception domain ที่ handler รองรับ
3. `JwtAuthenticationFilter` log exception (`JwtAuthenticationFilter.java:62-64`)
   ต้องระวังไม่ให้ token หรือข้อมูลลับหลุดใน log
4. ต้องตรวจเลขบรรทัดใหม่หลัง code freeze และเติม evidence ของ test ที่ผ่านจริง

