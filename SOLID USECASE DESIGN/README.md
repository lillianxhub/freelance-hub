# Authentication and User Documentation Draft

เอกสารชุดนี้เป็นร่างของ feature **Authentication และ User/Profile** สำหรับนำไป
รวมในเอกสารส่งอาจารย์ภายหลัง โดยอ้างอิงจาก implementation ปัจจุบันใน
`code/Backend/src/main/java/th/ac/kku/freelance_hub`

## ไฟล์ในชุดนี้

- `AUTH-SOLID.md` — วิเคราะห์ SOLID พร้อมไฟล์และบรรทัดอ้างอิง
- `AUTH-DESIGN-PATTERNS.md` — pattern ที่มี implementation จริงใน feature นี้
- `AUTH-USE-CASES.md` — use case, acceptance criteria และ flow ของ Auth/User

## ขอบเขตที่อ่านแล้ว

`AuthController`, `UserController`, `AuthService`, `AuthServiceImpl`,
`UserService`, `RevokedTokenService`, `User`, `UserProfile`, `RevokedToken`,
`UserRepository`, `UserProfileRepository`, `RevokedTokenRepository`,
`CustomUserDetailsService`, `JwtTokenProvider`, `JwtAuthenticationFilter`,
`JwtAuthenticationEntryPoint`, `SecurityConfig`, auth/user DTO, `UserMapper`,
exception handler และ auth/security tests

## วิธีนำไปรวม

เอกสารชุดนี้เป็น draft ของเจ้าของ feature ให้ reviewer ตรวจเทียบกับโค้ดและเลขบรรทัด
ก่อนคัดลอกเนื้อหาไปยัง `doc/solid-analysis.md`, `doc/design-patterns.md` และ
`doc/use-case-description.md` ฉบับกลาง ห้ามนำไฟล์นี้ไปแทนเอกสารกลางโดยตรงจนกว่าจะ
ตรวจ implementation รอบสุดท้ายแล้ว

## ข้อควรระวัง

- เอกสารนี้ไม่อ้างว่า Strategy, State หรือ Observer มีอยู่ใน Auth เพราะไม่พบ
  implementation ของสาม pattern นี้ในโค้ดส่วนที่อ่าน
- เลขบรรทัดเป็น baseline จาก commit ที่ตรวจครั้งนี้ ต้องตรวจใหม่หลังมีการแก้โค้ด
- Use case Invoice ไม่รวมอยู่ใน feature นี้และไม่อยู่ใน MVP

