# Use Case Description - Freelance Hub

ฉบับรวมสำหรับส่งรายวิชา CP353002 จากเอกสารสมาชิกทั้ง 5 คน ตรวจ endpoint และชื่อไฟล์กับ implementation ณ commit `dbcc4b9` วันที่ 10 ตุลาคม 2026

## 1. ขอบเขตระบบและ Actor

Freelance Hub เป็นระบบจัดการลูกค้า โปรเจกต์ งานย่อย เวลา และรายงานสำหรับ Freelancer แต่ละบัญชีมีข้อมูลของตนเอง ไม่รวม Finance, Invoice, Payment และ workspace ที่มีสมาชิกหลายคน

| Actor | Use cases |
|---|---|
| Guest | สมัครบัญชีและเข้าสู่ระบบ |
| Freelancer ที่เข้าสู่ระบบ | จัดการ profile/password, Client, Project, Task, Timer/Time Entry และ Dashboard/Reports |
| ผู้ใช้ที่มี refresh cookie | refresh session และ logout ภายใต้ origin validation |

Spring Security/JWT, repositories และ event listeners เป็นส่วนภายในระบบ ไม่ใช่ actor ภายนอกใน Use Case Diagram ปัจจุบันมี UserRole.ADMIN ใน model แต่ไม่มี HTTP use case ดูผู้ใช้รายคนด้วย `GET /api/users/{id}` จึงไม่นำ endpoint เก่าจากต้นฉบับมาอ้างในฉบับรวม

## 2. Contract และเงื่อนไขร่วม

- Business endpoints ต้องมี Bearer JWT; owner ID มาจาก CurrentUserProvider ไม่รับ owner ID จาก payload
- Service ตรวจ ownership ก่อนอ่านหรือแก้ไข ส่วน Refresh/Logout ใช้ HttpOnly cookie และตรวจ Origin/Referer ตาม TrustedOriginValidator
- API หลักที่มี body ใช้ ApiResult: `success`, `message`, `data`, `meta`, `error`
- Success มี `error=null`; errors ใช้ ApiErrorFactory โดยมี `code`, `status`, UTC `timestamp`, `traceId` และ optional `details`/`fieldErrors`
- RequestTraceFilter สร้าง trace ID ฝั่ง server และ header `X-Request-ID`; validation field errors อยู่ใน `error.fieldErrors` ไม่ใช่ `error.details`
- Pagination ของ Client/Project/Task/Time Entry/Reports เริ่ม page ที่ 1; meta มี page, limit, total และ totalPages
- Client DELETE และ Logout สำเร็จเป็น 204 ไม่มี body; Time Entry/Project/Task DELETE หลักคืน 200 ตาม contract ของ feature
- Task nested legacy routes บางเส้นยังคืน raw TaskResponse หรือ 204 ไม่ได้ใช้ envelope เหมือนเส้นหลัก จึงไม่อ้างว่า API ทุกเส้นเป็นรูปแบบเดียวกันแล้ว

## 3. Authentication และ User/Profile

ตรวจเทียบเอกสาร Petpinyo ใน PR #127 กับโค้ดจริง:

| ส่วนงาน | โค้ดอ้างอิง |
|---|---|
| HTTP / cookies / origin | [AuthController](../code/Backend/src/main/java/th/ac/kku/freelance_hub/controller/AuthController.java), [RefreshTokenCookie](../code/Backend/src/main/java/th/ac/kku/freelance_hub/security/RefreshTokenCookie.java), [TrustedOriginValidator](../code/Backend/src/main/java/th/ac/kku/freelance_hub/security/TrustedOriginValidator.java) |
| Login / register / refresh / logout | [AuthServiceImpl](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/impl/AuthServiceImpl.java), [RefreshTokenService](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/RefreshTokenService.java) |
| Profile / password / PATCH mapping | [UserController](../code/Backend/src/main/java/th/ac/kku/freelance_hub/controller/UserController.java), [UserService](../code/Backend/src/main/java/th/ac/kku/freelance_hub/service/UserService.java), [UserMapper](../code/Backend/src/main/java/th/ac/kku/freelance_hub/mapper/UserMapper.java) |

### Actors

| Actor | หน้าที่ |
|---|---|
| Guest | สมัครสมาชิกและเข้าสู่ระบบ |
| Authenticated User | ดู/แก้ profile และเปลี่ยนรหัสผ่านของตนเอง |
| User with refresh cookie | Refresh session และ logout; logout ยังเรียกโดยไม่มี cookie ได้ภายใต้ origin validation |

### Use Case Summary

| ID | Use Case | Actor | Endpoint | ผลลัพธ์ |
|---|---|---|---|---|
| UC-AUTH-01 | Register | Guest | `POST /api/auth/register` | สร้าง User + UserProfile, hash password, ตั้ง HttpOnly refresh cookie และคืน `ApiResult<AuthResponse>` พร้อม access JWT (`201`) |
| UC-AUTH-02 | Login | Guest | `POST /api/auth/login` | ตรวจ credentials, ตั้ง HttpOnly refresh cookie และคืน `ApiResult<AuthResponse>` พร้อม access JWT (`200`) |
| UC-AUTH-03 | Logout | User with refresh cookie | `POST /api/auth/logout` | เพิกถอน refresh-token family และคืน `204` |
| UC-AUTH-04 | Refresh | User with refresh cookie | `POST /api/auth/refresh` | หมุน cookie และคืน access JWT ใหม่ (`200`) |
| UC-USER-01 | View My Profile | Authenticated User | `GET /api/users/me` | คืนข้อมูล User + Profile ของตนเอง (`200`) |
| UC-USER-02 | Update My Profile | Authenticated User | `PATCH /api/users/me` | แก้ข้อมูล profile และที่อยู่ (`200`) |
| UC-USER-03 | Change Password | Authenticated User | `PATCH /api/users/me/password` | ตรวจ `oldPassword`, บันทึก `newPassword` เป็น hash (`200`) |

### UC-AUTH-01 Register

**Precondition:** Guest ยังไม่มี account ที่ใช้อีเมลเดียวกัน

**Main flow:**

1. ส่ง email, password, display name และข้อมูล profile ที่รองรับในการสมัคร
2. Controller ตรวจ `@Valid RegisterRequest`; password ต้องมีอย่างน้อย 8 ตัวอักษรและไม่เกิน 72 ไบต์ UTF-8
3. Service normalize email ด้วย trim/lowercase แล้วตรวจ `existsByEmail`
4. `PasswordEncoder` hash password
5. `UserMapper` สร้าง `UserProfile` และ `User.setProfile` link แบบ bidirectional
6. `UserRepository.save` บันทึก User + Profile ผ่าน cascade
7. สร้าง access JWT และ refresh token ที่เก็บเฉพาะ hash; คืน `AuthResponse` ใน `ApiResult` และ refresh token ใน HttpOnly cookie

**Alternative flow:** validation ไม่ผ่าน = `400`; email ซ้ำ = `409`; ระบบผิดพลาด = `500`

**Acceptance criteria:** password ห้ามเป็น plain text, email ถูก trim/lowercase ก่อนบันทึกและ email ซ้ำต่างตัวพิมพ์ต้องถูกปฏิเสธ,
response ห้ามเผย `passwordHash`, profile ต้องเชื่อมกับ user ถูกคน

### UC-AUTH-02 Login

**Precondition:** มี account และ request ผ่าน validation

**Main flow:** Controller เรียก `AuthService.login`; `AuthenticationManager` ตรวจ
credentials ผ่าน `DaoAuthenticationProvider`; service สร้าง access JWT อายุเริ่มต้น 15 นาที
และ refresh token สุ่มอายุเริ่มต้น 7 วัน ส่ง refresh token ใน HttpOnly cookie;
คืน `200 ApiResult<AuthResponse>` โดยไม่ส่ง refresh token ใน JSON

**Alternative flow:** validation ไม่ผ่าน = `400`; email/password ไม่ถูกต้อง = `401`;
เมื่อสะสม login ผิดครบ 10 ครั้งต่อ email ใน 15 นาที หรือครบ 300 ครั้งต่อนาทีต่อ IP คำขอถัดไป = `429`
พร้อม `Retry-After` และ `LOGIN_RATE_LIMITED`; นับเฉพาะ BadCredentialsException ไม่ใช่จำกัดทุก request ต่อ IP และ login สำเร็จล้างเฉพาะตัวนับ email

อายุ token ปรับได้ด้วย `jwt.expiration` และ `app.auth.refresh-expiration-ms` จึงไม่ใช่ค่าตายตัวทุก deployment; limiter เก็บใน memory ของแต่ละ instance ไม่ใช่ shared counter ข้าม server

### UC-AUTH-03 Logout

**Precondition:** ไม่ต้องมี access JWT หรือ refresh cookie แต่คำขอต้องผ่าน Origin/Referer validation

**Main flow:** Service เพิกถอน refresh-token family จาก cookie, ล้าง cookie และ
คืน `204 No Content`; logout ไม่เพิกถอน access JWT จึงยังใช้ได้จนหมดอายุ (ค่าเริ่มต้น 15 นาที)

**Alternative flow:** ไม่มี cookie หรือ logout ซ้ำยังคืน `204` แบบ idempotent;
`Origin`/`Referer` ไม่อยู่ใน allowlist หรือไม่มีทั้งคู่ = `403`

### UC-AUTH-04 Refresh

`POST /api/auth/refresh` รับ HttpOnly cookie ที่ Spring bind ให้ แล้วตรวจ `Origin`/`Referer` ก่อนเรียก service เช่นเดียวกับ logout จากนั้นหมุน token ใน transaction โดยคง `family_id` และวันหมดอายุเดิม สำเร็จตั้ง cookie ใหม่และคืน `200 ApiResult<AuthResponse>` หาก token ถูกใช้แล้วและยังไม่หมดอายุจะเพิกถอนทั้ง family โดย commit การเพิกถอนก่อนตอบ `401`; missing/invalid/expired/revoked token ได้ `401` เมื่อผ่าน origin validation แล้ว

### UC-USER-02 Update My Profile

1. ผู้ใช้เรียก `PATCH /api/users/me` พร้อม bearer JWT
2. `JwtAuthenticationFilter` ตรวจ token และใส่ authenticated principal ใน SecurityContext
3. `UserService` อ่าน user จาก principal ไม่รับ `userId` หรือ `ownerId` จาก body
4. Service โหลด User ผ่าน UserRepository แล้วให้ UserMapper แก้ profile และ embedded Address ใน `user_profiles`; ฟิลด์ที่ไม่ได้ส่งหรือเป็น null คงค่าเดิมตาม PATCH mapping ไม่ได้ใช้ UserProfileRepository ใน flow นี้
5. Controller คืน `200 ApiResult<UserResponse>` โดยแสดงข้อมูลที่อยู่เป็น flat fields

**Alternative flow:** ไม่มี/malformed JWT = `401`; validation ไม่ผ่าน = `400`

### UC-USER-03 Change Password

**Request body:**

```json
{
  "oldPassword": "รหัสผ่านเดิม",
  "newPassword": "รหัสผ่านใหม่"
}
```

1. ผู้ใช้เรียก `PATCH /api/users/me/password` พร้อม bearer JWT; รหัสผ่านใหม่ต้องมีอย่างน้อย 8 ตัวอักษรและไม่เกิน 72 ไบต์ UTF-8
2. Service โหลด user จาก authenticated principal และตรวจ `oldPassword` ด้วย `PasswordEncoder`
3. เมื่อถูกต้อง ระบบ hash และบันทึก `newPassword`; ห้ามบันทึกรหัสผ่านแบบ plain text
4. คืน `200 ApiResult<Void>`; old password ผิด = `401` และ `message: "รหัสผ่านไม่ถูกต้อง"`, validation หรือ new password ซ้ำค่าเดิม = `400`; เมื่อสำเร็จเพิกถอน refresh-token families ทั้งหมด

MVP นี้ไม่มี forgot/reset-password flow และไม่มีการอัปโหลดหรือเปลี่ยนรูปโปรไฟล์

### UC-USER-01 View My Profile

1. เรียก `GET /api/users/me` พร้อม JWT
2. `JwtAuthenticationFilter` ใส่ principal ใน SecurityContext
3. `UserService` อ่าน email จาก context และโหลด User
4. `UserMapper` รวมข้อมูล UserProfile เป็น `UserResponse`
5. คืน `200 ApiResult<UserResponse>`


### Sequence: Login และ protected request

ดู [Sequence 01: Login และ protected request](diagrams/sequence-diagram.md#scenario-01-login-and-protected-request)

## 4. Client Management

### Actor และเงื่อนไขร่วม

**Actor หลัก:** Freelancer ที่เข้าสู่ระบบด้วย JWT

**Precondition ร่วม:** Request มี bearer token ที่ถูกต้อง; Client ที่อ่าน/แก้ไข/เปลี่ยนสถานะ/soft delete ต้องเป็นของผู้ใช้คนนั้นและยังไม่ถูก soft delete
**กติกาการเป็นเจ้าของ:** Controller เรียก `CurrentUserProvider.currentUserId()` เพื่ออ่าน UUID ของ authenticated user ไม่รับ owner ID จาก payload และการหา Client รายตัวใช้ทั้ง `clientId` และ `ownerId`; UserService เป็น production implementation ของ provider

**Response ร่วม:** ทุก endpoint ที่มี body คืน `ApiResult` (`success`, `message`, `data`, `meta`, `error`); success มี `error=null`; error มี `success=false`, `data=null`, `meta=null` และ message อยู่ชั้นบน ส่วน `DELETE` สำเร็จเป็น `204 No Content` ไม่มี body

Error contract หลัง PR #107:

| ฟิลด์ | ความหมาย |
|---|---|
| `error.code` | รหัสที่ใช้แยกประเภท เช่น VALIDATION_ERROR, CLIENT_NOT_FOUND, AUTHENTICATION_REQUIRED |
| `error.details` | รายละเอียด error ที่ไม่ใช่ validation เช่น `{ "field": "id" }` เมื่อ UUID ไม่ถูกต้อง; อาจเป็น null |
| `error.status` | HTTP status เช่น 400, 401 หรือ 404 |
| `error.timestamp` | เวลาที่สร้าง error ในรูป ISO 8601 UTC ลงท้าย Z |
| `error.fieldErrors` | รายฟิลด์ของ validation เช่น `{ "name": "Client name is required" }`; ไม่ใส่ซ้ำใน details และเป็น null สำหรับ error ที่ไม่ใช่ validation |
| `error.traceId` | UUID ที่ server สร้าง ตรงกับ response header X-Request-ID |

`GlobalExceptionHandler` และ Security entry points ใช้ `ErrorHandlerChain` เลือก `ErrorDescriptor` แล้วให้ `ApiErrorFactory` สร้าง error ตาม [Error Contract](error-contract.md); `RequestTraceFilter` สร้าง trace ID ก่อน security/MVC ไม่ใช้ trace ID ที่ผู้เรียกส่งมาเป็นตัวระบุของระบบ

### Use Case Summary

| ID | Use Case | Endpoint | ผลลัพธ์หลัก | Requirement |
|---|---|---|---|---|
| UC-CLI-01 | Create Client | `POST /api/clients` | สร้าง Client โดย `isActive=true`; คืน `201 ApiResult<ClientResponse>` และ `Location` | FR-CLI-01, FR-CLI-02 |
| UC-CLI-02 | List/Search Clients | `GET /api/clients` | คืนรายการของ owner พร้อม `totalTrackedSeconds` รายลูกค้า, filter, sort, pagination และ `meta` (`200`) | FR-CLI-03 |
| UC-CLI-03 | View Client | `GET /api/clients/{id}` | คืน `ApiResult<ClientResponse>` พร้อม `totalTrackedSeconds`; เลือกแนบ Project/Task ด้วย `include` ได้ (`200`) | FR-CLI-01, FR-CLI-04 (บางส่วน) |
| UC-CLI-04 | Replace Client | `PUT /api/clients/{id}` | แทนที่ข้อมูลที่แก้ไขได้; optional fields ที่ไม่ส่งมาถูกล้าง (`200`) | FR-CLI-01, FR-CLI-02 |
| UC-CLI-05 | Update Client | `PATCH /api/clients/{id}` | แก้เฉพาะฟิลด์ที่ส่งมาและคืนข้อมูลล่าสุด (`200`) | FR-CLI-01, FR-CLI-02 |
| UC-CLI-06 | Change Client Status | `PATCH /api/clients/{id}/status` | กำหนด `isActive`; เมื่อเป็น false ให้ Project ที่ยังไม่ถูก soft delete เป็น `ARCHIVED` ด้วย โดยไม่ตั้ง `deletedAt` (`200`) | FR-CLI-01, FR-CLI-05 |
| UC-CLI-07 | Soft-delete Client | `DELETE /api/clients/{id}` | ตั้ง `deletedAt` โดยไม่เปลี่ยน `isActive` และไม่ลบ record (`204`) | งานเสริมของทีม; ข้อจำกัด FR-CLI-05 ดูด้านล่าง |

### UC-CLI-01 Create Client

1. Freelancer ส่งชื่อ Client และข้อมูลติดต่อ/บริษัท/ที่อยู่/เลขผู้เสียภาษี/หมายเหตุที่ต้องการ โดยที่อยู่ใช้ flat fields `address`, `subdistrict`, `district`, `province`, `postalCode`
2. Controller ตรวจ `CreateClientRequest` ด้วย `@Valid` และอ่าน owner จากผู้ใช้ที่ล็อกอิน
3. Service โหลด owner, ให้ mapper สร้าง `Client` แล้ว repository บันทึก
4. ระบบคืน `201 Created`, `ApiResult` ที่มี `data: ClientResponse` และ `Location: /api/clients/{id}`

**Alternative flow:** ไม่มี JWT = `401`; ชื่อว่างหรือข้อมูลผิดรูปแบบ = `400`

**Postcondition:** มี Client ใหม่ที่ผูกกับ owner ปัจจุบันและสถานะเริ่มต้น `ACTIVE`

### UC-CLI-02 List/Search Clients

1. Freelancer เรียก `GET /api/clients` พร้อม query parameter ที่ต้องการ: `status`, `search`, `page`, `size` หรือ `limit`, `sortBy`, `direction`; ถ้าส่งทั้ง `size` และ `limit` จะใช้ `limit`
2. Service สร้าง query ที่จำกัด `ownerId` และ `deletedAt IS NULL` ก่อนเสมอ แล้วเพิ่ม filter `isActive` ตาม `status` หรือ prefix search ใน `name`, `companyName`, `email`, `phone` และข้อมูลที่อยู่เมื่อระบุ; ที่อยู่เป็น embedded fields ในตาราง `clients`
3. Repository คืน `Page<Client>`; mapper แปลงรายการเป็น `ClientResponse` และ service รวมเวลาของ IDs ในหน้าปัจจุบันด้วย aggregate query เดียวเพื่อเติม `totalTrackedSeconds` ให้แต่ละลูกค้า (ไม่มีเวลา = `0`; หน้าว่างไม่เรียก aggregate query)
4. Controller คืน `200 ApiResult` พร้อม `PaginationMeta`; ทั้ง query `page` และ `meta.page` เริ่มที่ 1 โดย service แปลงเป็น index เริ่มที่ 0 สำหรับ Spring Data ภายใน และยอดรวมเวลาไม่เปลี่ยนจำนวนลูกค้าหรือการแบ่งหน้า

ค่าเริ่มต้น: `page=1`, `size=20`, `sortBy=name`, `direction=ASC`; `limit`/`size` ต้องอยู่ใน 1–100, `sortBy` รับ `name`, `companyName`, `email`, `createdAt`, `updatedAt` การค้นหา trim หัวท้ายและ escape `%`/`_` แต่ใช้ LIKE แบบ case-sensitive บน PostgreSQL ไม่ใช่ ILIKE

**Alternative flow:** ไม่ระบุ status = รวมทั้ง `isActive=true/false` ที่ยังไม่ถูก soft delete; ไม่มีผลลัพธ์ = `data` เป็นรายการว่าง; filter/page/size/limit/sort ไม่ถูกต้อง = `400`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูล และไม่แสดง Client ของผู้ใช้อื่น

### UC-CLI-03 View Client

1. Freelancer ส่ง UUID ของ Client ไปที่ `GET /api/clients/{id}`
2. Service ค้นด้วย `findByIdAndOwnerId` แล้ว mapper สร้าง `ClientResponse`; ถ้าระบุ `include=projects` จะอ่านเฉพาะข้อมูล Project ที่เกี่ยวข้อง หรือใช้ `include=projects.tasks` เพื่อแนบ Task ภายใต้ Project (รองรับการคั่นหลาย path ด้วยจุลภาค)
3. Service เติม `totalTrackedSeconds` ด้วยหลักเดียวกับรายการลูกค้า ทั้งกรณีส่งและไม่ส่ง `include`; controller คืน `200 ApiResult<ClientResponse>` รวม `status` ที่คำนวณจาก `isActive` และข้อมูลที่อยู่แบบ flat fields; หากไม่ส่ง `include` จะไม่แนบ `projects`

**Alternative flow:** ไม่พบ Client หรือเป็นของผู้ใช้อื่น = `404` แบบเดียวกัน; `include` ที่ไม่รองรับ เช่น `tasks` หรือ `Projects` = `400`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูล

`include` ใช้รูปแบบ path คล้าย JSON:API แต่ response ยังคงเป็น `ApiResult` ของทีม ไม่ใช่เอกสาร JSON:API เต็มรูปแบบ และยังไม่แนบเวลาที่ใช้ในแต่ละ Project

- `include=projects` คืน `projects[]` ที่มี `id`, `name`, `color`, `status`, `targetMinutes`; ไม่แนบ `tasks`
- `include=projects.tasks` คืน Project fields เดียวกันพร้อม `tasks[]` (`id`, `name`, `status`); Task ต้องผ่าน Project ของ Client นั้นและไม่มี pagination แยก
- Project และ Task ใน include ตัดด้วย `deletedAt IS NULL` ไม่กรองสถานะ: Project ที่ ARCHIVED หรือ Task ที่ COMPLETED แต่ไม่ถูก soft delete ยังแนบมาได้; Project เรียง name/id และ Task เรียง sortOrder/id ภายใน Project
- `include` นี้มีเฉพาะ Client detail ไม่ใช่พารามิเตอร์ของ Client list; ไม่พบ Project/Task ให้คืน array ว่างในส่วนที่ขอ

### ยอดเวลารวมใน Client GET responses

`totalTrackedSeconds` เป็นยอดรวม `durationSeconds` ตลอดช่วงเวลาที่มีข้อมูลของลูกค้านั้น มีใน `GET /api/clients` และ `GET /api/clients/{id}` โดยไม่มีการปัดเป็นนาที หากไม่มีข้อมูลให้คืน `0`; ไม่ได้เพิ่มคอลัมน์ในฐานข้อมูล และไม่เติมฟิลด์นี้ใน response ของ POST/PUT/PATCH

- ยอดต้องตรงกับ `GET /api/time-entries/summary?clientId={id}` ของผู้ใช้เดียวกันเมื่อไม่ส่งตัวกรองวันที่หรือชนิดรายการเพิ่มเติม
- ใช้ Time Entry ของ owner ที่ `isActive=true`, `endedAt IS NOT NULL` และ `durationSeconds IS NOT NULL`; ไม่รวม timer ที่ยังรันหรือ Time Entry ที่ถูก soft delete แต่ยังรวมรายการที่ล็อกแล้วและประวัติบน Project/Task ที่ archive หรือ soft delete เช่นเดียวกับ summary ปัจจุบัน
- ใช้ `ClientRepository.sumTrackedSecondsByClientIds` ดึงยอดแยกตาม Client ด้วย query เดียวสำหรับทั้งหน้า; service เติม `0` ให้ลูกค้าที่ไม่มีผลลัพธ์ โดยไม่เรียก summary ทีละคน

### ข้อมูลเวลาแยกตามลูกค้าสำหรับ Dashboard/Analytics

`ClientService.summarizeTimeByClient(ownerId, fromInclusive, toExclusive)` เป็น method ภายในให้ service อื่นเรียก ไม่ใช่ Client HTTP endpoint โดยคืน `ClientTimeTotalResponse(clientId, clientName, totalSeconds)` เรียงเวลามากไปน้อย

method นี้ยังใช้กติกาเดิมสำหรับ Analytics ซึ่งต่างจาก `totalTrackedSeconds` ใน Client GET responses จึงไม่ได้ใช้คำนวณฟิลด์ใหม่

- Query รวม `durationSeconds` ของ Time Entry ที่จบแล้วและยังไม่ถูกลบ ผ่านความสัมพันธ์ Time Entry → Project → Client โดยจำกัด owner และไม่รวม Time Entry/Project/Task ที่ inactive หรือถูก soft delete
- กรองด้วย `startedAt` ในช่วง `[fromInclusive, toExclusive)`; ผู้เรียกต้องแปลงวันที่ในหน้าจอเป็น `Instant` ตาม timezone ที่ต้องการก่อน
- Project ที่ไม่ได้ผูก Client จะไม่อยู่ในผลลัพธ์นี้; การแสดงกลุ่ม “Self Project/อื่น ๆ” และการแปลงหน่วยเวลาเป็นหน้าที่ของ Dashboard/Analytics

### UC-CLI-04 Replace Client

1. Freelancer ส่ง UUID และรายละเอียดที่จะแทนที่ด้วย `PUT /api/clients/{id}` โดยใช้ `CreateClientRequest`
2. Service ตรวจ owner และให้ mapper แทนที่ข้อมูลที่แก้ไขได้ทั้งหมด; optional fields ที่ไม่ส่งมาถูกล้าง
3. Controller คืน `200 ApiResult<ClientResponse>` โดยไม่เปลี่ยน owner, `isActive` หรือ `deletedAt`

**Alternative flow:** ชื่อหรือข้อมูลไม่ถูกต้อง = `400`; ไม่พบ/ไม่ใช่เจ้าของ/ถูก soft delete แล้ว = `404`; ไม่มี JWT = `401`
**Postcondition:** ข้อมูลที่แก้ไขได้ถูกแทนที่ แต่สถานะและการเป็นเจ้าของคงเดิม

### UC-CLI-05 Update Client

1. Freelancer ส่ง UUID และฟิลด์ที่ต้องการแก้ด้วย `PATCH /api/clients/{id}`
2. Controller validate `UpdateClientRequest`; service ตรวจ owner ผ่าน `findByIdAndOwnerId`
3. Mapper คงค่าเดิมเมื่อฟิลด์เป็น `null` และส่งค่าที่ระบุไปแก้ entity
4. Repository บันทึก แล้ว controller คืน `200 ApiResult<ClientResponse>`

**Alternative flow:** ข้อมูลไม่ถูกต้อง = `400`; ไม่พบ/ไม่ใช่เจ้าของ = `404`; ไม่มี JWT = `401`

**Postcondition:** เฉพาะข้อมูลที่ส่งมาได้รับการแก้ไข; `isActive`/`status` ไม่ใช่ฟิลด์ของ PATCH รายละเอียดนี้

ข้อมูลที่อยู่ของ Client ใช้ contract เดียวกับ User Profile และ response ยังคงแสดงเป็น flat fields
และ persistence เก็บเป็น embedded columns ในตาราง `clients` ไม่ได้อ้างอิงตาราง `addresses`

### UC-CLI-06 Change Client Status

1. Freelancer ส่ง `{"isActive": true}` หรือ `{"isActive": false}` ไปที่ `PATCH /api/clients/{id}/status`
2. Service ตรวจ owner; ถ้า `isActive=false` ให้ repository โหลด Project ของลูกค้าและ owner นั้นที่ `deletedAt IS NULL` ตรวจว่าไม่มี timer ของผู้ใช้กำลังทำงานใน Project เหล่านั้นก่อน หากมีให้ตอบ `409`; จากนั้นเรียก `Project.changeStatus(ARCHIVED)` ให้แต่ละรายการมี `status=ARCHIVED` และ `isActive=false` โดยไม่ตั้ง `deletedAt` และไม่เปลี่ยน Task/Time Entry
3. Service เรียก `Client.setActive(isActive)` และบันทึก Client พร้อม Project ใน transaction เดียวกัน; เมื่อ `isActive=true` จะไม่คืนสถานะ Project อัตโนมัติ
4. Controller คืน `200 ApiResult<ClientResponse>`; `status` ใน response คำนวณเป็น `ACTIVE` หรือ `ARCHIVED` จาก `isActive`

**Alternative flow:** มี timer ทำงานใน Project ของลูกค้านี้ขณะจัดเก็บ = `409`; ไม่ส่ง `isActive` = `400`; ไม่พบ/ไม่ใช่เจ้าของ/ถูก soft delete แล้ว = `404`; ไม่มี JWT = `401`
**Postcondition:** เมื่อจัดเก็บ Client ให้ Project ที่ผูกอยู่และยังไม่ถูก soft delete เป็น `ARCHIVED` ด้วย รวมรายการที่เคย `COMPLETED`; เรียกจัดเก็บซ้ำได้ โดยไม่ลบประวัติและไม่กระทบลูกค้าหรือผู้ใช้อื่น

ใช้ `Project.changeStatus(ARCHIVED)` สำหรับการเปลี่ยนสถานะนี้ ไม่ใช้ `Project.archive()` ซึ่งตั้ง `deletedAt` ด้วย

### UC-CLI-07 Soft-delete Client

1. Freelancer ส่ง UUID ไปที่ `DELETE /api/clients/{id}`
2. Service ตรวจ owner แล้วเรียก `Client.softDelete()` เพื่อตั้ง `deletedAt` โดยไม่เปลี่ยน `isActive`
3. Repository บันทึกโดยไม่ลบแถว; controller คืน `204 No Content`

**Alternative flow:** ไม่พบ/ไม่ใช่เจ้าของ/ถูก soft delete แล้ว = `404`; ไม่มี JWT = `401`
**Postcondition:** Client และประวัติที่ผูกอยู่ยังคงอยู่; Client ที่ soft delete แล้วไม่ปรากฏใน Client API แม้ไม่กรอง status แต่ค่า `isActive` เดิมไม่เปลี่ยน

**Requirement boundary:** FR-CLI-05 ระบุว่าห้ามลบลูกค้าที่มีธุรกรรม แต่ implementation ของ soft delete ยังไม่ตรวจว่ามีธุรกรรมหรือไม่ จึงยังไม่อ้างว่า DELETE พิสูจน์ requirement นี้ครบ; ถ้าทีมกำหนดว่าห้ามเฉพาะ hard delete และอนุญาต soft delete ต้องบันทึกการตีความนั้นใน requirement หลักก่อน

### Sequence: Soft-delete Client

ดู [Sequence 04: Soft-delete Client](diagrams/sequence-diagram.md#scenario-04-soft-delete-client)

Sequence นี้แสดง flow หลังผ่าน JWT แล้ว; RequestTraceFilter สร้าง X-Request-ID/MDC ก่อนหน้าและล้าง MDC เมื่อคำขอจบ ส่วน 401 ถูกตอบจาก security ก่อนถึง controller

### หลักฐานข้าม feature: BR-06

การเริ่ม timer ใหม่เมื่อ Client ถูก archive หรือ soft delete ถูกป้องกันใน `TimeEntry.startTimer()` ผ่าน `requireTrackableProject()` ที่ตรวจว่า Client มี `isActive=true` และ `deletedAt=null`; มี test `rejectsTimerForArchivedClient` ใน `domain/entity/TimeEntryTest.java` และ `service/TimerServiceImplTest.java` สำหรับกรณี archive แล้ว เป็นหลักฐานจาก Time Tracking ไม่ใช่การอ้างว่า Client endpoint ตรวจการเริ่ม timer เอง

ขอบเขตสำคัญ: guard นี้ตรวจ `Client.deletedAt` โดยตรง จึงปฏิเสธการเริ่ม timer บน Project ของ Client ที่ถูก soft delete แม้ Client ยังมี `isActive=true` อยู่ Client soft delete ยังคง isActive เดิมและไม่ archive Project หรือหยุด running timer ที่มีอยู่ ส่วนการ archive Client ตรวจ running timer และปฏิเสธด้วย `409` หากยังมี timer ทำงาน ไม่หยุด timer ให้อัตโนมัติ

### ขอบเขตที่ยังไม่เสร็จ

- `FR-CLI-04` ยังครบไม่หมด: Client detail แนบ Project/Task แบบเลือกได้แล้ว แต่ยังไม่คืนเวลาที่ใช้ในแต่ละ Project; method รวมเวลาปัจจุบันรวมตาม Client สำหรับ Dashboard/Analytics ไม่ใช่เวลาราย Project ในหน้า Client detail
- `FR-CLI-05` ยังต้องยืนยันการตีความ soft delete กับทีม หรือเพิ่มกติกาตรวจธุรกรรมตาม requirement; เอกสารนี้ไม่เปลี่ยนพฤติกรรม DELETE ให้เอง
- การค้นหาใน `FR-CLI-03` เป็น prefix search จากชื่อ บริษัท อีเมล เบอร์โทร และที่อยู่ตามรูปแบบที่บันทึกไว้; ยังไม่ใช่การค้นหาแบบตัดช่องว่างหรือเครื่องหมายในเบอร์โทร
- มี [Use Case Diagram](diagrams/use-case-diagram.md) แล้ว โดยใช้ actors/IDs ของฉบับรวม; diagram ไม่ถือว่าปิด requirement gaps ของ Client ที่ระบุข้างต้น

**หลักฐานการทดสอบ:** `ClientControllerTest`, `ClientServiceImplTest`, `ClientRepositoryTest` และ `ClientIntegrationTest` ภายใต้ `code/Backend/src/test/java/th/ac/kku/freelance_hub/`

- Controller tests mock CurrentUserProvider โดยตรงและติดตั้ง RequestTraceFilter; ตรวจ envelope ของ 400/404/409/500 รวม timestamp/traceId และไม่เปิดเผยข้อความ exception ภายในใน 500
- Integration tests ตรวจ 401 ของทุก Client endpoint ผ่าน Spring Security, validation fieldErrors, 404, UUID/include/page ที่ไม่ถูกต้อง และ trace ID ที่ต่างกันแต่ละ request
- OpenAPI test ตรวจ ApiError fields และ `allOf` ของ Client error response ที่กำหนด success=false; test profile ใช้ H2 และไม่เปิด Flyway จึงไม่ใช่หลักฐาน PostgreSQL migration

## 5. Project และ Task Management

### Actor และกติกาที่ใช้ร่วมกัน

**Actor หลัก:** Freelancer ที่เข้าสู่ระบบ ระบบอ่าน `ownerId` จากผู้ใช้ปัจจุบัน ไม่รับจาก request และตรวจความเป็นเจ้าของก่อนอ่านหรือแก้ Project/Task โดย ID ที่ส่งผ่าน API เป็น UUID

เส้น Project และ Task หลักคืนรูปแบบ `ApiResult` (`success`, `message`, `data`, `meta`, `error`) รายการที่แบ่งหน้าจะใส่ `page`, `limit`, `total`, `totalPages` ใน `meta` ส่วนเส้น Task แบบซ้อนบางเส้นที่ยังเปิดใช้อยู่คืน `TaskResponse` ตรง ๆ ตามโค้ดปัจจุบัน

เมื่อเกิดข้อผิดพลาดจากเส้นหลัก ระบบคืน `ApiResult` ที่มี `success=false`, `data=null` และ `error` ซึ่งระบุ `code`, HTTP `status`, `timestamp` และ `traceId`; กรณี validation มี `fieldErrors` เพิ่มเติม

### Use Case Summary

| ID | Use Case | Endpoint หลัก | ผลลัพธ์ |
|---|---|---|---|
| UC-PRJ-01 | สร้าง Project | `POST /api/projects` | `201`, `ApiResult<ProjectResponse>` และ `Location` |
| UC-PRJ-02 | ค้นหา/แสดงรายการ Project | `GET /api/projects` | `200`, รายการ `ProjectListItemResponse` และ `meta` |
| UC-PRJ-03 | ดู Project รายตัว | `GET /api/projects/{id}` | `200`, `ProjectListItemResponse` |
| UC-PRJ-04 | แก้รายละเอียด Project | `PUT /api/projects/{id}` | `200`, `ProjectResponse` |
| UC-PRJ-05 | เปลี่ยนสถานะ Project | `PATCH /api/projects/{id}/status` | `200`, `ProjectResponse` |
| UC-PRJ-06 | ลบ Project แบบ soft delete | `DELETE /api/projects/{id}` | `200`, `ApiResult` |
| UC-TSK-01 | สร้าง Task ใน Project | `POST /api/projects/{projectId}/tasks` | `201`, `TaskResponse` และ `Location` |
| UC-TSK-02 | แสดงรายการ Task ใน Project | `GET /api/projects/{projectId}/tasks` | `200`, รายการ `TaskResponse` และ `meta` |
| UC-TSK-03 | ดู Task รายตัว | `GET /api/tasks/{taskId}` | `200`, `TaskResponse` |
| UC-TSK-04 | แก้ Task | `PUT /api/tasks/{taskId}` | `200`, `TaskResponse` |
| UC-TSK-05 | เปลี่ยนสถานะ Task | `PATCH /api/tasks/{taskId}/status` | `200`, `TaskResponse` |
| UC-TSK-06 | เรียงลำดับ Task | `PATCH /api/projects/{projectId}/tasks/reorder` | `200`, `TaskResponse` |
| UC-TSK-07 | ลบ Task แบบ soft delete | `DELETE /api/tasks/{taskId}` | `200`, `ApiResult` |

### Project use cases

#### UC-PRJ-01 สร้าง Project

1. ส่ง `clientId` และ `name` (บังคับ) พร้อม `description`, `startDate`, `endDate`, `color` รูปแบบ `#RRGGBB` และ `targetMinutes` (ถ้ามี)
2. Service ตรวจว่า Client เป็นของผู้ใช้ สร้าง Project ด้วยสถานะเริ่มต้น `PLANNED` แล้วบันทึก
3. คืน `201 Created` พร้อม `Location: /api/projects/{id}` และ `ApiResult<ProjectResponse>`

**ทางเลือก:** request ไม่ถูกต้อง = `400`; ไม่พบ Client ของผู้ใช้ = `404`

#### UC-PRJ-02 ค้นหา/แสดงรายการ Project

1. เรียก `GET /api/projects` พร้อม `search` (ชื่อ Project หรือ Client), `clientId`, `status`, `page`, `limit`, `sortBy`, `direction` หรือ `include=tasks`
2. `page` เริ่มที่ 1 ค่าเริ่มต้นคือหน้า 1 ครั้งละ 20 รายการ เรียง `project_name` แบบ `ASC`; `sortBy` รับ `project_name`, `update_at`, `end_date`
3. ไม่ส่ง `status` จะไม่แสดง `ARCHIVED`; `status=ARCHIVED` แสดงเฉพาะสถานะนั้น; `status=ALL` แสดงทุกสถานะรวม `ARCHIVED` แต่ทุกกรณียังตัด Project ที่ `deletedAt` ไม่เป็น null ออก
4. `include=tasks` จะแนบเฉพาะ Task ที่ยังใช้งานของแต่ละ Project; `taskProgress` สรุปจำนวน Task และเปอร์เซ็นต์ที่เสร็จ ส่วน `timeTracking` ส่ง `trackedSeconds`, `trackedHours`, `usagePercent` จาก Time Entry ที่บันทึกเวลาจบแล้วให้อัตโนมัติ ไม่ต้องส่ง `include` เพื่อขอเวลา
5. คืน `ApiResult<List<ProjectListItemResponse>>` พร้อม `meta` สำหรับการแบ่งหน้า

ตัวอย่าง: `GET /api/projects?page=1&limit=10&sortBy=project_name&direction=ASC&status=ALL&include=tasks`

**ทางเลือก:** ไม่มีผลลัพธ์ = `data` เป็นรายการว่าง; ตัวกรองหรือ pagination ไม่ถูกต้อง = `400`
**Postcondition:** ไม่แก้ข้อมูลและไม่แสดง Project ของผู้ใช้อื่น

#### UC-PRJ-03 ดู Project รายตัว

1. ส่ง UUID ของ Project; Service ค้นด้วย `projectId` และ `ownerId` โดยไม่คืนรายการที่ soft delete แล้ว
2. คืน `ProjectListItemResponse` พร้อมข้อมูล Client, `taskProgress` และ `timeTracking` แบบเดียวกับรายการ Project; ไม่มี `recentTimeEntries` ใน response นี้

**ทางเลือก:** ไม่พบ Project หรือไม่ใช่เจ้าของ = `404`

#### UC-PRJ-04 แก้รายละเอียด Project

1. ส่ง `PUT /api/projects/{id}` โดย request ต้องมี `clientId` และ `name`; ส่งรายละเอียดอื่นได้เหมือนตอนสร้าง
2. Service ตรวจ Project ของผู้ใช้ก่อน หากเป็น `ARCHIVED` จะปฏิเสธการแก้ไข; กรณีที่แก้ได้จึงตรวจ Client อัปเดตรายละเอียด และคืน `ApiResult<ProjectResponse>`

**ทางเลือก:** request ไม่ถูกต้อง = `400`; ไม่พบ Project/Client ของผู้ใช้ = `404`; Project เป็น `ARCHIVED` = `409`
**Postcondition:** รายละเอียดเปลี่ยน แต่การเปลี่ยนสถานะเป็น use case แยก

#### UC-PRJ-05 เปลี่ยนสถานะ Project

1. ส่ง `PATCH /api/projects/{id}/status` พร้อม `status` ใหม่; Service ค้นเฉพาะ Project ของผู้ใช้ที่ยังไม่ถูก soft delete แล้วตรวจว่าไม่มี timer ของ Project นี้กำลังทำงานก่อนเปลี่ยนสถานะ
2. หากเปลี่ยนจาก `ACTIVE` เป็น `COMPLETED`, Service ใช้ `TaskRepository.summarizeProgressByProjectIds()` นับเฉพาะ Task ที่ยังใช้งาน (`isActive=true`) และปฏิเสธถ้ายังมี Task ที่ไม่เป็น `COMPLETED`; Project ที่ไม่มี Task ที่ยังใช้งานไม่ถูกเงื่อนไขนี้ปฏิเสธ การเปลี่ยนเป็น `ARCHIVED` ไม่ต้องรอให้ Task เสร็จ
3. `Project.changeStatus()` ให้ State ของสถานะปัจจุบันตรวจ transition ก่อนบันทึก: `PLANNED → ACTIVE/ARCHIVED`, `ACTIVE → ON_HOLD/COMPLETED/ARCHIVED`, `ON_HOLD → ACTIVE/ARCHIVED`, `COMPLETED → ARCHIVED`, `ARCHIVED → ACTIVE/PLANNED`; การคืนจาก `ARCHIVED` ทำได้ต่อเมื่อ Project ยังไม่ถูก soft delete และ Client ยังใช้งาน (`isActive=true`, `deletedAt=null`); ส่งสถานะเดิมซ้ำได้
4. ถ้าเพิ่งเปลี่ยนจากสถานะอื่นเป็น `COMPLETED`, Service เรียก `TimeEntryService.lockByProject(ownerId, projectId)` ก่อนบันทึก Project ภายใน transaction เดียวกัน รายการเวลาที่มีอยู่ของ Project จะถูกล็อกและแก้หรือลบไม่ได้; การส่ง `COMPLETED` ซ้ำไม่ล็อกซ้ำ
5. เมื่อเป็น `ARCHIVED` จะตั้ง `isActive=false`; เมื่อเปลี่ยนจาก `ARCHIVED` กลับ `ACTIVE` หรือ `PLANNED` จะตั้ง `isActive=true` การเปลี่ยนสถานะนี้ไม่ตั้ง `deletedAt`

**ทางเลือก:** สถานะไม่ถูกต้อง = `400`; ไม่พบ Project = `404`; transition ผิดกฎ, Client ถูกจัดเก็บ/soft delete, timer ของ Project กำลังทำงาน หรือ `ACTIVE → COMPLETED` ขณะที่ยังมี Task ที่ยังใช้งานและไม่เสร็จ = `409`

**Postcondition ของการเปลี่ยนเป็น COMPLETED:** รายการเวลาที่ถูกล็อกแล้วจะถูก API ปฏิเสธหากพยายามแก้หรือลบ (`409`)

#### UC-PRJ-06 ลบ Project แบบ soft delete

1. ส่ง `DELETE /api/projects/{id}`; Service ตรวจเจ้าของและตรวจว่าไม่มี timer ของ Project นี้กำลังทำงาน แล้วเรียก `Project.archive()`
2. Entity ตั้งสถานะ `ARCHIVED`, `isActive=false` และ `deletedAt` แล้วคืน `200 ApiResult` โดย `data=null`

**ทางเลือก:** ไม่พบ Project หรือไม่ใช่เจ้าของ = `404`; timer ของ Project กำลังทำงาน = `409`
**Postcondition:** แถวยังอยู่ในฐานข้อมูล แต่ไม่ปรากฏใน Project list แม้ส่ง `status=ALL` และไม่สามารถเรียก Project API เพื่อคืนสถานะได้ เพราะ query ค้นด้วย ID ตัดรายการที่ `deletedAt` ไม่เป็น null ออก

### Task use cases

#### UC-TSK-01 สร้าง Task

ส่ง `name`, `sortOrder` (เริ่มที่ 0) และ `description` ถ้ามี Service ตรวจเจ้าของ Project และ `project.canEditTasks()` จากนั้นแทรก Task ในตำแหน่งที่ระบุ จัดลำดับ Task ที่ยังใช้งาน แล้วคืน `201 ApiResult<TaskResponse>` พร้อม `Location: /api/tasks/{taskId}` หากตำแหน่งไม่ถูกต้องได้ `400`; หากสถานะ Project ห้ามแก้ Task ได้ `409`

#### UC-TSK-02 แสดงรายการ Task

`GET /api/projects/{projectId}/tasks` รับ `is_active` (ค่าเริ่มต้น `true`), `page` (เริ่มที่ 1), `limit` และ `sort` คืน `ApiResult<List<TaskResponse>>` พร้อม `meta` หากไม่กำหนด sort จะเรียง `sortOrder` จากน้อยไปมาก

#### UC-TSK-03 ดู Task รายตัว

`GET /api/tasks/{taskId}` ตรวจเจ้าของผ่าน Project แล้วคืน `ApiResult<TaskResponse>`; route เก่า `GET /api/projects/{projectId}/tasks/{taskId}` ถูกคอมเมนต์ปิดแล้ว

#### UC-TSK-04 แก้ Task

`PUT /api/tasks/{taskId}` รับ `name` และ `description` ตรวจ `project.canEditTasks()` แล้วคืน `ApiResult<TaskResponse>` หาก Project เป็น `COMPLETED` หรือ `ARCHIVED` จะไม่อนุญาตให้แก้ (`409`)

#### UC-TSK-05 เปลี่ยนสถานะ Task

`PATCH /api/tasks/{taskId}/status` ใช้ enum ของ Task เอง (`OPEN`, `IN_PROGRESS`, `COMPLETED`) ไม่ใช้ `ProjectStatus`: ผ่าน API ใช้ `OPEN → IN_PROGRESS`, `IN_PROGRESS → COMPLETED` และ `COMPLETED → IN_PROGRESS` เมื่อย้อนงาน; ย้อนเป็น `OPEN` ไม่ได้ `TaskServiceImpl` ปฏิเสธ `OPEN → COMPLETED` ทั้งเส้นนี้และเส้นเดิม `/complete` ด้วย `409` ก่อนเรียก `Task.changeStatus()` (ตัว Entity ยังไม่ได้ห้าม transition นี้เอง) การย้อนจะล้าง `completedAt` เป็น `null` และหากทำเสร็จอีกครั้งจะบันทึกเวลาใหม่ การส่งสถานะเดิมซ้ำจะไม่เปลี่ยนข้อมูล Service ตรวจ `project.canEditTasks()` ก่อนเปลี่ยนสถานะ จึงไม่ให้ย้อน Task ใน Project ที่ `COMPLETED` หรือ `ARCHIVED` (`409`)

#### UC-TSK-06 เรียงลำดับ Task

`PATCH /api/projects/{projectId}/tasks/reorder` รับ `taskId` กับ `sortOrder` ซึ่งเริ่มจาก 0 Service ตรวจเจ้าของและสิทธิ์แก้ Task ย้ายตำแหน่งเฉพาะ Task ที่ยังใช้งาน แล้วคืน `ApiResult<TaskResponse>`

#### UC-TSK-07 ลบ Task แบบ soft delete

`DELETE /api/tasks/{taskId}` ตรวจเจ้าของและ `project.canEditTasks()` ก่อนตั้ง `isActive=false`, `deletedAt` และจัดลำดับ Task ที่เหลือใหม่ โดยเก็บ Time Entry เดิมไว้ คืน `200 ApiResult` โดย `data=null`

**เส้นเดิมที่ยังเปิดอยู่:** `PATCH /api/projects/{projectId}/tasks/{taskId}` (แก้), `PATCH /api/projects/{projectId}/tasks/{taskId}/complete` (ปิด), `PATCH /api/projects/{projectId}/tasks/{taskId}/reorder` (ย้าย) และ `DELETE /api/projects/{projectId}/tasks/{taskId}` ยังทำงาน แต่บางเส้นคืน `TaskResponse` ตรง ๆ หรือ `204` ต่างจากเส้นหลักด้านบน ส่วน route `/start` ถูกคอมเมนต์ปิดแล้ว

### ข้อมูลสำหรับ Dashboard และความคืบหน้าเวลา

- `ProjectService.countActiveAndCompleted(ownerId)` คืนจำนวน `ACTIVE`, `COMPLETED` และผลรวมผ่าน `totalCount()`; เป็น service method ไม่ใช่ endpoint ใหม่
- `ProjectService.getProgress(ownerId, projectId)` คืน `targetMinutes`, `trackedSeconds`, `progressPercent` และระดับ `NO_TARGET`, `BELOW_80`, `REACHED_80`, `REACHED_100`; ถ้าไม่มีเป้าหมายยังคืนเวลาที่บันทึกได้ แต่เปอร์เซ็นต์เป็น `null`
- `TaskService.getLatestTimeEntryTaskName(ownerId)` อ่านชื่อ Task ของ Time Entry ล่าสุดตาม `startedAt` และคืน `Optional.empty()` หากไม่มีรายการหรือรายการล่าสุดไม่ผูก Task; เป็น service method ไม่ใช่ endpoint ใหม่
- เมื่อหยุด timer ระบบรับ `TimerStoppedEvent` หลัง transaction commit แล้วตรวจว่าข้ามเกณฑ์ 80%/100% หรือไม่ จากนั้นเผยแพร่ `ProjectProgressThresholdEvent`; listener ปัจจุบัน **บันทึก log เท่านั้น** ยังไม่มี notification ที่ส่งถึงผู้ใช้หรือเก็บลงฐานข้อมูล

### Sequence: เปลี่ยนสถานะ Project

ดู [Sequence 03: เปลี่ยนสถานะ Project](diagrams/sequence-diagram.md#scenario-03-change-project-status)

### ขอบเขตปัจจุบัน

- เอกสารนี้อธิบาย Project/Task และ listener ความคืบหน้าที่เกี่ยวกับ Project เท่านั้น ไม่อธิบายการทำงานทั้งหมดของ Timer, Time Entry หรือ Dashboard
- `FR-PRJ-07` มีการตรวจเกณฑ์และเผยแพร่ event แล้ว แต่การแจ้งเตือนถึงผู้ใช้จริงยังไม่ปรากฏในส่วนนี้
- การล็อกรายการเวลาเกิดเมื่อเปลี่ยนเข้าสู่ `COMPLETED` ครั้งใหม่; Project ที่เป็น `COMPLETED` อยู่ก่อนเพิ่ม flow นี้ไม่ได้ถูกล็อกย้อนหลังโดยอัตโนมัติ
- เมื่อ Client ถูกตั้ง inactive, `ClientServiceImpl.changeStatus()` เปลี่ยน Project ที่ยังไม่ถูก soft delete ของ Client เป็น `ARCHIVED` โดยตรง ไม่ผ่านการตรวจ running timer ของ `ProjectServiceImpl`; การ soft delete Client ไม่เปลี่ยนสถานะ Project อัตโนมัติ จึงเป็นคนละ flow กับ UC-PRJ-05/06
- Project list เรียก `timeTracking()` ซึ่งเรียก `TimeEntryService.summarize()` แยกแต่ละ Project และ summary โหลด Time Entry มารวมใน Java ปัจจุบัน จึงยังไม่ใช่ aggregate query เดียวต่อหน้า และเป็นจุดที่ควรปรับหากข้อมูลเพิ่มขึ้น

## 6. Time Tracking

### Actor และเงื่อนไขร่วม

**Actor หลัก:** Freelancer ที่เข้าสู่ระบบด้วย JWT

**Precondition ร่วม:** Request มี bearer token ที่ถูกต้อง

**กติกาการเป็นเจ้าของ:** ระบบดึง owner ID จาก authenticated user ไม่รับ owner ID จาก request และค้นหา Project, Task หรือ Time Entry ภายใต้ owner คนนั้นเสมอ

### Use Case Summary

| ID | Use Case | Endpoint | ผลลัพธ์หลัก | Requirement |
|---|---|---|---|---|
| UC-TIME-01 | Start Timer | `POST /api/timer/start` | สร้าง running timer และคืน `201` พร้อม `StartedTimerResponse` ใน `data` | FR-TIME-01, FR-TIME-02, FR-TIME-03 |
| UC-TIME-02 | View Current Timer | `GET /api/timer/current` | คืน `200` พร้อม `CurrentTimerResponse` ทั้งกรณีมีและไม่มี timer | FR-TIME-01, FR-TIME-02 |
| UC-TIME-03 | Stop Timer | `POST /api/timer/stop` | บันทึกเวลาสิ้นสุด คำนวณวินาที และคืน `200` พร้อม `StoppedTimerResponse` | FR-TIME-01, FR-TIME-03, FR-TIME-08 |
| UC-TIME-04 | Cancel Timer | `DELETE /api/timer/current` | ลบ running timer และคืน `200` พร้อม `data: null` | FR-TIME-01 |
| UC-TIME-05 | Create Manual Time Entry | `POST /api/time-entries` | สร้าง completed entry และคืน `201` พร้อม `TimeEntryDetailResponse` | FR-TIME-03, FR-TIME-04 |
| UC-TIME-06 | List and Filter Time Entries | `GET /api/time-entries` | คืน `200` พร้อมรายการใน `data` และ pagination ใน `meta` | FR-TIME-06, FR-TIME-07 |
| UC-TIME-07 | Summarize Time Entries | `GET /api/time-entries/summary` | คืน `200` พร้อมจำนวนรายการและ `totalSeconds` ของรายการที่จบแล้ว | FR-TIME-08 |
| UC-TIME-08 | Update Time Entry | `PUT /api/time-entries/{id}` | แทนข้อมูลรายการที่ไม่ถูกล็อกและคืน `200` พร้อม `TimeEntryDetailResponse` | FR-TIME-05 |
| UC-TIME-09 | Delete Time Entry | `DELETE /api/time-entries/{id}` | soft delete completed entry ที่ไม่ถูกล็อกและคืน `200` พร้อม `data: null` | FR-TIME-05 |
| UC-TIME-10 | View Time Entry Detail | `GET /api/time-entries/{id}` | คืน `200` พร้อม `TimeEntryDetailResponse` ของ owner | FR-TIME-06 |
| UC-TIME-11 | Lock Project Time Entries | ไม่มี endpoint; เรียก `TimeEntryService.lockByProject(ownerId, projectId)` ภายใน Backend | ตั้ง `lockedAt` ถาวรให้รายการของ Project รวม soft-deleted โดยรักษาเวลาล็อกเดิม | กติกาการล็อกเมื่อ Project เปลี่ยนเป็น `COMPLETED` |

ทุก endpoint ใน UC-TIME-01 ถึง UC-TIME-10 คืน `ApiResult` รูปแบบ `{success, message, data, meta, error}` โดย `message` ของ Time Tracking เป็นภาษาไทย; `meta` มีข้อมูล pagination เฉพาะ list ส่วน error ที่ controller จัดการคืน `success: false` และรหัสใน `error.code` ยกเว้น `401` ซึ่งจัดการโดยระบบ authentication ส่วนกลาง; UC-TIME-11 เป็นคำสั่งภายใน Backend จึงไม่มี HTTP response ของตัวเอง

Response ที่มี `project` หรือ `task` ส่งสถานะปัจจุบันใน `project.status` และ `task.status` ด้วย; หากไม่ได้ระบุ Task จะคืน `task: null` ส่วน `StoppedTimerResponse` ไม่มี object ทั้งสอง

ชื่อ Task ใน nested DTO ของ Timer/Time Entry ใช้ `task.title` ไม่ใช่ `task.name` (ต่างจาก TaskResponse และ Client include); list DTO ไม่ส่ง entryType/lockedAt แม้ service response ภายในมีข้อมูลเหล่านี้

### UC-TIME-01 Start Timer

1. Freelancer ส่ง `projectId`, optional `taskId` และ optional `description`
2. Controller validate `StartTimerRequest` และอ่าน owner ID จากผู้ใช้ที่เข้าสู่ระบบ
3. Service ตรวจว่า User และ Project มีอยู่จริง โดย Project ต้องเป็นของ owner
4. หากส่ง Task ระบบตรวจว่า Task อยู่ใน Project และเป็นของ owner คนเดียวกัน
5. Entity ตรวจว่า Project สามารถจับเวลาได้ (`canTrackTime()`) และ Client มี `isActive = true`, `deletedAt = null` โดยใช้เวลาจาก server ผ่าน `Clock`
6. Service ตรวจว่า owner ยังไม่มี running timer แล้วบันทึกรายการชนิด `TIMER`; หากมี Task จะเรียก `Task.start()` โดย `OPEN` เปลี่ยนเป็น `IN_PROGRESS`, `IN_PROGRESS` คงเดิม และ `COMPLETED` ถูกปฏิเสธ การบันทึก Timer และการเปลี่ยนสถานะ Task อยู่ใน transaction เดียวกัน
7. Controller คืน `201 Created`, `ApiResult<StartedTimerResponse>` และ `Location: /api/time-entries/{id}` โดย `data` มี `project` และ `task` พร้อม `status` ของแต่ละรายการ

**Alternative flow:** ไม่มี JWT = `401`; Project/Task ไม่พบหรือไม่ใช่ของ owner = `404`; Project ไม่สามารถจับเวลาได้หรือ Client inactive/ถูก soft delete = `409`; Task เป็น `COMPLETED` = `409` และ transaction ย้อนกลับ; มี running timer อยู่แล้ว = `409`; request ไม่ถูกต้อง = `400`\
**Postcondition:** มี Time Entry ชนิด `TIMER` ที่มี `startedAt` แต่ยังไม่มี `endedAt` และ `durationSeconds`; owner มี running timer ได้ไม่เกินหนึ่งรายการ; Task ที่ส่งมามีสถานะ `IN_PROGRESS`

### UC-TIME-02 View Current Timer

1. Freelancer เรียก `GET /api/timer/current`
2. Service ค้นหารายการชนิด `TIMER` ของ owner ที่ยังไม่มี `endedAt`
3. Controller แปลงผลเป็น `CurrentTimerResponse` โดยกรณีพบ timer จะได้ `running = true` และข้อมูลใน `timeEntry`
4. Controller คืน `200 OK` ภายใน `ApiResult`

**Alternative flow:** ไม่มี JWT = `401`; ไม่มี running timer = `200` พร้อม `running = false` และ `timeEntry = null`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูล

### UC-TIME-03 Stop Timer

1. Freelancer เรียก `POST /api/timer/stop`
2. Service ค้นหา running timer ของ owner ด้วย pessimistic write lock ภายใน transaction
3. Entity กำหนด `endedAt` จาก server clock และคำนวณ `durationSeconds` เป็นจำนวนวินาทีเต็มโดยไม่ปัดขึ้นเป็นนาที
4. Service เผยแพร่ `TimerStoppedEvent`; หลัง transaction commit แล้ว `TimerStoppedProgressListener` จึงตรวจเกณฑ์ความคืบหน้าโปรเจกต์และเผยแพร่ `ProjectProgressThresholdEvent` หากถึง 80% หรือ 100%
5. Controller คืน `200` พร้อม `StoppedTimerResponse` ใน `ApiResult.data`

**Alternative flow:** ไม่มี JWT = `401`; ไม่มี running timer = `404`

**Postcondition:** Timer กลายเป็น completed entry และมี `startedAt`, `endedAt` และ `durationSeconds`

### UC-TIME-04 Cancel Timer

1. Freelancer เรียก `DELETE /api/timer/current`
2. Service ค้นหาและล็อก running timer ของ owner ภายใน transaction
3. Repository ลบรายการดังกล่าว
4. Controller คืน `200 OK` พร้อม `ApiResult` ที่มี `data: null`

**Alternative flow:** ไม่มี JWT = `401`; ไม่มี running timer = `404`

**Postcondition:** Running timer ถูกลบโดยไม่สร้าง completed entry และไม่เผยแพร่ `TimerStoppedEvent`

### UC-TIME-05 Create Manual Time Entry

1. Freelancer ส่ง `projectId`, optional `taskId`, optional `description`, `startedAt` และเลือกส่งอย่างใดอย่างหนึ่งระหว่าง `endedAt` หรือ `durationSeconds`
2. Controller validate ว่ามีวิธีกำหนดเวลาสิ้นสุดเพียงแบบเดียวและเวลาสิ้นสุดอยู่หลังเวลาเริ่ม
3. Service ตรวจ User, Project, Task และ owner relationship; Project ต้องเป็น `ACTIVE` และ Client ต้อง active และไม่ถูก soft delete จึงบันทึกเวลาได้
4. Entity สร้างรายการชนิด `MANUAL`; หากส่ง duration ระบบคำนวณ `endedAt` หรือหากส่งช่วงเวลาระบบคำนวณ duration
5. หากส่ง Task จะเรียก `Task.start()` ก่อนบันทึก: `OPEN` เปลี่ยนเป็น `IN_PROGRESS`, `IN_PROGRESS` คงเดิม และ `COMPLETED` ถูกปฏิเสธ; การบันทึก Time Entry และการเปลี่ยนสถานะ Task อยู่ใน transaction เดียวกัน
6. Repository บันทึก แล้ว controller คืน `201 Created` พร้อม `Location` และ `TimeEntryDetailResponse` ใน `ApiResult.data` ซึ่งมี `createdAt`, `updatedAt`, `project.status` และ `task.status` เมื่อมี Task

**Alternative flow:** ไม่มี JWT = `401`; Project/Task ไม่พบหรือไม่ใช่ของ owner = `404`; Project ไม่เป็น `ACTIVE`, Client inactive/ถูก soft delete หรือ Task เป็น `COMPLETED` = `409` และไม่บันทึกรายการ; ไม่ส่งหรือส่งทั้ง `endedAt` และ `durationSeconds` = `400`; duration ไม่เป็นบวกหรือช่วงเวลาไม่ถูกต้อง = `400`
**Postcondition:** มี completed manual entry ที่ duration มากกว่า 0 บน Project ที่เป็น `ACTIVE`; Task ที่ส่งมามีสถานะ `IN_PROGRESS`

### UC-TIME-06 List and Filter Time Entries

1. Freelancer เรียก `GET /api/time-entries` พร้อม query parameter ที่ต้องการ ได้แก่ `clientId`, `projectId`, `taskId`, `entryType`, `from`, `to`, `page`, `limit`, `sortBy` และ `direction`
2. `TimeEntryService.list()` เริ่มเงื่อนไขด้วย owner ID และ `isActive = true` เสมอ แล้วจึงเพิ่ม filter ที่ส่งมา
3. ช่วงเวลาใช้ `from` แบบ inclusive และ `to` แบบ exclusive กับ `startedAt`
4. Repository คืน `Page<TimeEntry>` และ mapper แปลงเป็น `Page<TimeEntryResponse>`
5. Controller คืน `200 OK` พร้อม `TimeEntryListItemResponse` ใน `data` ซึ่งมี `description` ของแต่ละรายการ รวมถึง timer ที่หยุดแล้ว และ `{page, limit, total, totalPages}` ใน `meta` โดย `page` เริ่มที่ 1

List ยังรวม running timer ที่เข้า filter ด้วย (`endedAt`/`durationSeconds` เป็น null); การตัด running timer ออกใช้เฉพาะ summary ไม่ใช่ list

**Alternative flow:** ไม่มีผลลัพธ์ = `data` เป็นรายการว่าง; `from` ไม่น้อยกว่า `to`, page/limit, sort field หรือ direction ไม่ถูกต้อง = `400`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูลและไม่แสดง Time Entry ของผู้ใช้อื่น

### UC-TIME-07 Summarize Time Entries

1. Freelancer เรียก `GET /api/time-entries/summary` พร้อม filter ชุดเดียวกับรายการ
2. `TimeEntryService.summarize()` จำกัดข้อมูลด้วย owner, `isActive = true` และ filter ที่ระบุ; `from` รวมขอบล่าง ส่วน `to` ไม่รวมขอบบน โดยเทียบกับ `startedAt`
3. ระบบนับเฉพาะรายการที่มี `endedAt` และ `durationSeconds` จึงไม่นับ running timer หรือรายการที่ soft delete
4. ระบบคืน `entryCount`, `totalSeconds`, `from` และ `to` ใน `TimeEntrySummaryResponse` ภายใต้ `ApiResult.data`; `page`, `limit` และการ sort ไม่เปลี่ยนผลรวม

**Alternative flow:** ไม่มีรายการที่เข้าเงื่อนไข = `entryCount` และ `totalSeconds` เป็น `0`; ช่วงเวลาไม่ถูกต้อง = `400`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูลและไม่มีการคำนวณมูลค่าเงิน

### UC-TIME-08 Update Time Entry

1. Freelancer ส่ง UUID ของรายการใน path และข้อมูลทดแทนผ่าน `PUT` ได้แก่ `projectId`, `startedAt`, optional `taskId`/`description` และอย่างใดอย่างหนึ่งระหว่าง `endedAt` หรือ `durationSeconds`
2. Controller validate ข้อมูลที่จำเป็นและเวลาที่ส่งมา; `taskId` ที่ไม่ส่งหรือเป็น `null` จะล้าง Task เดิม และ `description` ที่ไม่ส่งหรือเป็น `null` จะล้างคำอธิบายเดิม
3. Service ค้นหารายการด้วย `entryId` และ `ownerId` แล้วตรวจว่าไม่ถูกล็อก
4. ระบบตรวจว่า Project เดิมและ Project ปลายทางเป็น `ACTIVE` และ Client ปลายทาง active และไม่ถูก soft delete; หากเปลี่ยน Project หรือ Task จะตรวจ owner และ task-project relationship อีกครั้ง
5. Entity แก้ข้อมูลและคำนวณ `durationSeconds` ใหม่ตามช่วงเวลาหรือค่าที่ส่งมา
6. Controller คืน `200` พร้อม `TimeEntryDetailResponse` ใน `ApiResult.data`

**Alternative flow:** ไม่พบหรือเป็นของผู้ใช้อื่นหรือถูก soft delete = `404`; รายการถูกล็อก = `409`; Project เดิมหรือปลายทางไม่เป็น `ACTIVE` หรือ Client ปลายทางไม่ active = `409`; ช่วงเวลาไม่ถูกต้องหรือ request ไม่ครบ = `400`; พยายามแก้ running timer = `409`; Project/Task ไม่ถูกต้อง = `404`; ไม่มี JWT = `401`
**Postcondition:** ข้อมูลที่ส่งมาแทนค่าเดิม; ไม่ส่ง Task จะล้าง Task เดิม

### UC-TIME-09 Delete Time Entry

1. Freelancer ส่ง UUID ไปที่ `DELETE /api/time-entries/{id}`
2. Service ค้นหารายการด้วย `entryId` และ `ownerId`
3. Service ปฏิเสธรายการที่ถูกล็อกหรือ timer ที่ยังทำงาน
4. Entity กำหนด `isActive = false` และ `deletedAt` จาก server clock แล้วบันทึก; controller คืน `200 OK` พร้อม `data: null`

**Alternative flow:** ไม่พบหรือเป็นของผู้ใช้อื่น = `404`; รายการถูกล็อก = `409`; รายการเป็น running timer = `409` และต้องใช้ cancel timer flow; ไม่มี JWT = `401`

**Postcondition:** Completed entry ยังอยู่ในฐานข้อมูล แต่ไม่ปรากฏใน list, detail และ summary ปกติ

### UC-TIME-10 View Time Entry Detail

1. Freelancer เรียก `GET /api/time-entries/{id}` พร้อม UUID ของรายการ
2. `TimeEntryService.getById()` ค้นหาเฉพาะรายการที่เป็นของ owner และ `isActive = true`
3. Controller คืน `200 OK` พร้อม `TimeEntryDetailResponse` ใน `ApiResult.data` รวม `createdAt` และ `updatedAt`

**Alternative flow:** ไม่พบ เป็นของผู้ใช้อื่น หรือถูก soft delete = `404`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูล

### UC-TIME-11 Lock Project Time Entries

1. `ProjectServiceImpl.changeStatus()` ตรวจสิทธิ์เจ้าของและไม่ให้มี running timer ใน Project ก่อนเปลี่ยนสถานะ; เมื่อเปลี่ยนเป็น `COMPLETED` จะเรียก `TimeEntryService.lockByProject(ownerId, projectId)` ใน transaction เดียวกัน โดยไม่มีคำสั่ง lock จากหน้าบ้าน
2. `TimeEntryServiceImpl` ใช้ `@Transactional(propagation = Propagation.MANDATORY)` เพื่อบังคับว่าผู้เรียกต้องเปิด transaction ไว้แล้ว
3. Repository ใช้ `findLockedByOwnerIdAndProjectIdAndLockedAtIsNull` พร้อม `PESSIMISTIC_WRITE` เพื่อดึงเฉพาะรายการที่ยังไม่ล็อกของ owner/Project ที่ระบุ รวมรายการที่ soft delete และ timer ที่ยังวิ่งอยู่
4. Service ตรวจทุกรายการก่อนแก้ข้อมูล; หากพบ running timer จะโยน `InvalidStateException` โดยไม่หยุด timer อัตโนมัติและไม่ตั้ง `lockedAt` ให้รายการใด
5. หากไม่มี running timer ระบบอ่านเวลาจาก `Clock` ครั้งเดียว แล้วเรียก `TimeEntry.lock(lockedAt)` กับทุกรายการที่พบและ flush ภายใน transaction ของผู้เรียก
6. รายการที่ล็อกอยู่แล้วไม่ถูกแก้และรักษา `lockedAt` เดิม; ไม่มีรายการที่ต้องล็อกสามารถจบการทำงานได้

**Alternative flow:** ไม่มี transaction = `IllegalTransactionStateException`; ไม่ส่ง owner/project ID = `NullPointerException` ก่อน query; พบ running timer = `InvalidStateException` ซึ่งผู้เรียกต้องปล่อยให้ transaction ย้อนกลับ โดย HTTP response เป็นหน้าที่ของ API ฝั่งผู้เรียก\
**Postcondition:** เมื่อ transaction commit รายการที่ถูกเลือกมี `lockedAt` ถาวรและไม่สามารถแก้ไขหรือ soft delete ผ่าน Entity/service ปกติได้; การล็อกแถวฐานข้อมูลสิ้นสุดเมื่อ transaction จบ แต่ค่า `lockedAt` ยังอยู่

**สถานะการเชื่อมต่อ:** `ProjectServiceImpl.changeStatus()` เรียกเมธอดล็อกเมื่อ Project เปลี่ยนเป็น `COMPLETED` แล้ว; integration test ตรวจว่าการเปลี่ยนสถานะผ่าน API ตั้ง `lockedAt` ในฐานข้อมูล และการแก้ไข/ลบ Time Entry หลังจากนั้นได้ `409 TIME_ENTRY_LOCKED` การสร้าง manual entry หรือ PUT ย้ายรายการเข้า Project ที่ไม่เป็น `ACTIVE` ถูกปฏิเสธ

### Sequence: Start และ Stop Timer

ดู [Sequence 02: Start และ Stop Timer](diagrams/sequence-diagram.md#scenario-02-start-and-stop-timer)

### ขอบเขตที่ยังไม่เสร็จ

- `FR-TIME-06` รองรับรายวันและรายสัปดาห์ผ่านการส่งขอบเขต `from/to` แต่ยังไม่มี endpoint ที่จัดกลุ่มผลลัพธ์เป็นวันหรือสัปดาห์โดยตรง
- `BR-07` ใช้ `Instant` สำหรับเวลา UTC แต่การแสดงผลตาม timezone ของผู้ใช้เป็นหน้าที่ของ client และยังไม่มี user-timezone conversion ใน Time Tracking API
- มี `TimeEntryService.lockByProject()` สำหรับล็อกถาวรตาม Project รวม soft-deleted แล้ว โดยไม่มี API ให้หน้าบ้านสั่ง lock; ฝั่ง Project เรียกเมธอดนี้เมื่อเปลี่ยนเป็น `COMPLETED` ใน transaction เดียวกันแล้ว แต่ยังต้องจัดการ concurrent creation/reassignment
- เมธอดล็อกครอบคลุมรายการที่มีอยู่ขณะเรียกเท่านั้น; การสร้าง manual entry และ PUT ไปยัง Project ที่ไม่เป็น `ACTIVE` จะถูกปฏิเสธ
- Audit event สำหรับการแก้ไข Time Entry ตาม non-functional requirement ยังไม่ได้แสดงใน implementation นี้

**หลักฐานการทดสอบ:** `TimerControllerTest`, `TimeEntryControllerTest`, `TimerServiceImplTest`, `TimeEntryServiceImplTest` (รวมกลุ่ม `Queries` สำหรับงานอ่าน), `TimeEntryRepositoryTest`, `ProjectServiceImplTest` และ `TimeEntryIntegrationTest` ภายใต้ `code/Backend/src/test/java/th/ac/kku/freelance_hub/`

การล็อกมี 4 unit test cases ใน `TimeEntryServiceImplTest` สำหรับ manual/completed timer/soft-deleted ด้วย server clock, ปฏิเสธ running timer ก่อนล็อก, ไม่มีรายการ และ ID ที่จำเป็น; อีก 2 cases ใน `TimeEntryRepositoryTest` ตรวจการบันทึกจริง การแยก owner/Project การรักษาเวลาล็อกเดิมและเรียกซ้ำ รวมถึงการปฏิเสธเมื่อไม่มี transaction โดยใช้ฐานข้อมูล H2 ใน test profile; `ProjectServiceImplTest` ตรวจจุดเรียกเมื่อเปลี่ยนเป็น `COMPLETED` และ `TimeEntryIntegrationTest` ตรวจ flow ผ่าน API จนถึง `lockedAt` และข้อผิดพลาด `409` เมื่อแก้ไขหรือลบ

## 7. Dashboard และ Reports

### Actor และเงื่อนไขร่วม

**Actor หลัก:** ผู้ใช้ที่เข้าสู่ระบบ (Freelancer)

**Precondition:** มี access token ที่ใช้งานได้; route อยู่หลัง `ProtectedRoute`

**ขอบเขตข้อมูล:** Backend อ่าน owner ID จากผู้ใช้ที่ authenticate แล้ว ไม่รับ owner ID จาก query string

### Use Case Summary

| ID | Use Case | Endpoint ที่หน้าเรียก | ผลลัพธ์หลัก |
|---|---|---|---|
| UC-ANA-01 | เปิด Dashboard | `GET /api/dashboard`, `GET /api/timer/current` | KPI, กราฟสัปดาห์, โปรเจกต์ที่กำลังทำ, งานที่ต้องทำ, timer และเวลาล่าสุด |
| UC-ANA-02 | เปลี่ยนช่วงกราฟ Dashboard | `GET /api/dashboard/activity?period=MONTH\|YEAR` | กราฟเดือนหรือปีจากช่วงที่เลือก; `WEEK` ใช้ข้อมูลจาก Dashboard response |
| UC-ANA-03 | หยุด timer จาก Dashboard | `POST /api/timer/stop` แล้ว refresh current timer และ dashboard | เปลี่ยนจาก timer ที่กำลังทำงานเป็นรายการเวลาล่าสุด |
| UC-ANA-04 | เปิด Reports | `GET /api/reports/summary`, `/distribution`, `/projects` | KPI, กราฟเวลาตามลูกค้า, กราฟเทียบเป้าหมาย และตารางโปรเจกต์ |
| UC-ANA-05 | กรอง Reports และเลือกการจัดกลุ่มกราฟเวลา | สามเส้นเดียวกับ UC-ANA-04 พร้อม `from`, `to`, `clientId`, `projectId`, `status`; `/distribution` รับ `groupBy=CLIENT\|PROJECT` | แสดงข้อมูลตามตัวกรองและสลับกราฟเวลาตามลูกค้าหรือโปรเจกต์ได้ |
| UC-ANA-06 | เปลี่ยนหน้าตาราง Reports | `GET /api/reports/projects?page=N&limit=10` | ใช้ `meta` เพื่อแสดงรายการและกราฟโปรเจกต์ของหน้านั้น |
| UC-ANA-07 | ส่งออก CSV จาก Reports | ไม่มี request เพิ่ม | ดาวน์โหลดรายการโปรเจกต์ของหน้าตารางปัจจุบัน |
| UC-ANA-08 | อ่านแนวโน้มเวลารายงานผ่าน API | `GET /api/reports/work-trend` | คืนจุดเวลาแบบ DAY/WEEK/MONTH; ยังไม่มีส่วนแสดงผลบนหน้า Reports |
| UC-ANA-09 | อ่านรูปแบบการทำงานผ่าน API | `GET /api/reports/work-pattern` | คืนวัน/ชั่วโมงที่ทำงานมากที่สุดและข้อมูลกระจายเวลา; ยังไม่มีส่วนแสดงผลบนหน้า Reports |

ทุก GET API คืน wrapper `{success, message, data, meta, error}`; `/reports/projects` ใช้ `meta` สำหรับ pagination ส่วน current timer เป็น endpoint ที่ Dashboard ใช้ร่วมกับ Topbar โดยไม่ถือว่าการพัฒนา Timer API เป็นขอบเขต Analytics

### UC-ANA-01 เปิด Dashboard

1. ผู้ใช้เปิด `/dashboard`; `DashboardProvider` โหลด `/api/dashboard` และ `TimerProvider` มี current timer ส่วนกลาง
2. หน้าแสดง KPI 4 ใบ: เวลาที่บันทึกสัปดาห์นี้และเทียบสัปดาห์ก่อน, การใช้ชั่วโมงเป้าหมาย, จำนวนโปรเจกต์ active, งานที่เสร็จแล้วต่อทั้งหมด
3. กราฟเริ่มต้นเป็น `WEEK` โดยอ่าน `dailyWork` ย้อนหลัง 7 วันจาก dashboard response และแสดงเวลาเป็นวินาที/นาที/ชั่วโมงตามขนาดค่า; KPI สัปดาห์นี้ใช้ช่วงตั้งแต่วันจันทร์ถึงสิ้นวันนี้ตาม Asia/Bangkok
4. ตารางโปรเจกต์แสดงชื่อ ลูกค้า ความคืบหน้า จำนวนงานที่เสร็จ และสถานะ; ตารางงานแสดงงานที่ยังเปิดพร้อมลิงก์ไปหน้าโปรเจกต์
5. Card ตัวจับเวลาแสดง timer ที่กำลังวิ่งอยู่ หากไม่มีจะแสดงเวลาล่าสุด; ใต้ card แสดง `recentTimeEntries` จาก dashboard response

ขอบเขต Backend ปัจจุบัน: `activeProjects` ไม่เกิน 5 รายการเรียง taskProgressPercent มากไปน้อยแล้ว name; `openTasks` ไม่เกิน 5 รายการที่ไม่เป็น COMPLETED เรียง updatedAt ใหม่ก่อน; `recentTimeEntries` **ไม่เกิน 2 รายการ** ที่จบแล้ว เรียง startedAt ใหม่ก่อน (ไม่ใช่ endedAt และไม่ใช่ 5 รายการ)

KPI งานที่เสร็จนับ Task ที่ยังใช้งานของ Project ทุกสถานะที่ยังไม่ถูก soft delete ไม่ได้จำกัดเฉพาะ 5 Project ในตารางหรือเฉพาะ ACTIVE; KPI เป้าหมายรวมเวลาและ target เฉพาะ ACTIVE Project ที่ตั้ง targetMinutes ใช้เวลาตลอดประวัติ ไม่ใช่เฉพาะสัปดาห์นี้

**Alternative flow:** API หลักผิดพลาด แสดง `ErrorState` พร้อมปุ่มลองใหม่; ยังไม่มีรายการ แสดงข้อความว่าง; task ของ timer หรือรายการล่าสุดเป็น `null` แสดงได้โดยไม่ล้ม

**Postcondition:** ไม่มีการแก้ข้อมูล ผู้ใช้เห็นข้อมูลของ owner ตนเอง

### UC-ANA-02 เปลี่ยนช่วงกราฟ Dashboard

1. ผู้ใช้เลือก `WEEK`, `MONTH` หรือ `YEAR` จาก select
2. `WEEK` ใช้ `dailyWork` ที่โหลดไว้ ไม่เรียก activity endpoint เพิ่ม
3. `MONTH` หรือ `YEAR` เรียก `/api/dashboard/activity?period=...`
4. `lib/dashboard.ts` แปลงจุดกราฟและ label ก่อน `ProductivityChart` แสดงผล โดยคงหน่วยวินาทีไว้จนถึงขั้นจัดรูปแบบ

Activity API รับเพียง `period=WEEK|MONTH|YEAR` (ค่าเริ่มต้น WEEK), ไม่มี `/api/dashboard/chart?startDate=...&endDate=...` ตามข้อเสนอเก่า WEEK เป็น 7 วันล่าสุดรวมวันนี้; MONTH ส่งทุกวันในเดือนปัจจุบัน และ YEAR ส่ง 12 จุดรายเดือนของปีปัจจุบัน รวมช่วงที่ยังไม่มีข้อมูลด้วยค่า 0 ไม่ใช่กรองถึงเวลาปัจจุบันเสมอ

**Alternative flow:** โหลดกราฟไม่สำเร็จ แสดง error เฉพาะกราฟและปุ่มลองใหม่; ระหว่างรอแสดง loading; ค่า 0 แสดงเป็น 0 ได้

**Postcondition:** เปลี่ยนเฉพาะ state การแสดงผล ไม่มีข้อมูลถูกบันทึก

### UC-ANA-03 หยุด timer จาก Dashboard

1. ถ้ามี running timer, `DashboardTimerCard` แสดง project, optional task, description, เวลาเริ่มและเวลาที่เดินในรูป `HH:mm:ss`
2. ผู้ใช้กด “หยุดจับเวลา”; ระหว่างรอปุ่ม disabled
3. หน้าเรียก `stopTimer()` ที่ feature Timer มีอยู่แล้ว; งาน Analytics คือเชื่อม action เข้ากับการ์ด Dashboard
4. เมื่อสำเร็จ refresh `/api/timer/current` และ `/api/dashboard` เพื่อให้ card กับรายการเวลาล่าสุดตรงกัน

**Alternative flow:** หยุดไม่สำเร็จ แสดง Sonner toast สี error; card ยังคงสถานะที่อ่านได้ล่าสุด

**Postcondition:** เมื่อ Backend บันทึกสำเร็จ running timer กลายเป็นรายการเวลาที่จบแล้ว

### UC-ANA-04 เปิด Reports

1. ผู้ใช้เปิด `/reports`; วันที่เริ่มและสิ้นสุดว่างทั้งคู่ หมายถึงข้อมูลทุกช่วงเวลา
2. หน้าโหลด summary, distribution แบบ `CLIENT` และ projects หน้า 1 จำกัด 10 รายการ โดยไม่เรียก API ลูกค้า/โปรเจกต์/เวลาแยกมาเพื่อคำนวณเอง
3. summary เติม dropdown และ KPI: เวลารวม, จำนวน Time Entry, จำนวนโปรเจกต์ที่มีเวลาเทียบทั้งหมด, จำนวนลูกค้าที่มีเวลาเทียบทั้งหมด
4. distribution แสดงกราฟแท่งแนวนอน; projects แสดงกราฟแท่งเปอร์เซ็นต์การใช้ชั่วโมงเทียบเป้าหมายและความคืบหน้างาน พร้อมตารางชื่อโปรเจกต์ ลูกค้า เป้าหมาย เวลาที่ใช้ เปอร์เซ็นต์ และสถานะ

**Alternative flow:** ไม่มีข้อมูล แสดง 0/empty state; API ล้มเหลว แสดง error และปุ่มลองใหม่; เมื่อดูทุกช่วงเวลา ค่าแนวโน้มเทียบช่วงก่อนเป็น `null` จึงแสดง “ทุกช่วงเวลา”

**Postcondition:** ไม่มีการแก้ข้อมูล

### UC-ANA-05 กรอง Reports

1. ผู้ใช้เลือกวันที่ทั้งคู่ หรือเว้นว่างทั้งคู่; อาจเลือกลูกค้า โปรเจกต์ และสถานะ Project
2. หน้าไม่ส่งค่า `ALL` ไป Backend; เมื่อเปลี่ยนลูกค้าหรือสถานะ Project จะล้างโปรเจกต์ที่เคยเลือกและกลับไปหน้า 1; dropdown Project กรองด้วยทั้ง Client และสถานะ
3. เมื่อเปลี่ยน filter หน้าเรียก summary, distribution และ projects ด้วย filter เดียวกัน; เมื่อกดสลับกราฟตามลูกค้าหรือโปรเจกต์ หน้าเรียกเฉพาะ distribution ด้วย `groupBy=CLIENT` หรือ `PROJECT` ตามที่เลือก
4. การเปลี่ยนวันที่หรือโปรเจกต์กลับไปหน้า 1 ของตาราง

**Alternative flow:** กรอกวันที่ข้างเดียวหรือวันที่เริ่มหลังสิ้นสุด แสดงข้อความ error และไม่เรียก API ด้วยช่วงที่ผิด; filter UUID ไม่ใช่ของผู้ใช้หรือ Client/Project ไม่ตรงกัน Backend ตอบ 404; status ผิด enum ตอบ 400; Project ที่มีสิทธิ์อ่านแต่ไม่ตรง status ให้ผลว่าง

**Postcondition:** ผลรายงานถูกจำกัดตาม filter ที่เลือกโดยไม่เปลี่ยนข้อมูลต้นทาง

### UC-ANA-06 เปลี่ยนหน้าตาราง Reports

1. ผู้ใช้กด pagination ของตาราง
2. หน้าเรียก `/api/reports/projects?page=N&limit=10` พร้อม filter ปัจจุบัน
3. ใช้ `meta.page`, `meta.totalPages`, `meta.total` สำหรับปุ่ม pagination และจำนวนรายการ
4. ตารางและกราฟเทียบเป้าหมายเปลี่ยนเป็นโปรเจกต์ของหน้าปัจจุบัน

**Alternative flow:** ไม่มีโปรเจกต์ตาม filter แสดงแถว empty state; โหลดล้มเหลวแสดง error ให้ลองใหม่

**Postcondition:** ไม่มีการแก้ข้อมูล

### UC-ANA-07 ส่งออก CSV จาก Reports

1. เมื่อหน้าตารางมีข้อมูล ผู้ใช้กด “ส่งออกหน้านี้ CSV”
2. หน้าแปลงแถวปัจจุบันเป็น CSV ด้วย `downloadCsv()` โดย escape เครื่องหมายคำพูดและใส่ BOM สำหรับภาษาไทย
3. Browser ดาวน์โหลดไฟล์; ถ้าไม่มีวันที่ใน filter ใช้ชื่อ `time-report-all.csv`

**Alternative flow:** ไม่มีข้อมูลในหน้าตาราง หรือช่วงวันที่ไม่ถูกต้อง ปุ่ม disabled

**Postcondition:** ได้ไฟล์เฉพาะรายการในหน้าปัจจุบัน ไม่ใช่ Time Entry ทุกแถวหรือทุกหน้าของรายงาน

### UC-ANA-08 และ UC-ANA-09 Analytics API ที่ยังไม่มี UI

- `GET /api/reports/work-trend` รับ `from` และ `to` ครบคู่ พร้อม `granularity=DAY|WEEK|MONTH` แล้วคืนจุดกราฟตามหน่วยเวลานั้น; หากไม่ระบุช่วงวันที่ Backend ตอบ 400
- `GET /api/reports/work-pattern` รับ filter แบบเดียวกับ Reports และคืนชั่วโมงตามวันในสัปดาห์/ชั่วโมงในวัน พร้อมวันที่และช่วงเวลาที่มากที่สุด; เมื่อดูทุกช่วงเวลา `trackedTimeTrendPercent` เป็น `null`
- ทั้งสอง endpoint จำกัดข้อมูลด้วย owner ปัจจุบัน แต่ `ReportsPage` ยังไม่เรียก จึงไม่อ้างว่าเป็น UI ที่เสร็จแล้ว

### ตารางขอบเขตยอดเวลา

ทุกชุดด้านล่างจำกัด owner และนับเฉพาะ entry ที่จบแล้วและมี durationSeconds แต่เงื่อนไขความสัมพันธ์ไม่เหมือนกัน:

| ชุดข้อมูล | เงื่อนไขที่ query ตรวจจริง | ผลเมื่อ archive/soft delete ความสัมพันธ์ |
|---|---|---|
| Client GET totalTrackedSeconds / Time Entry summary / Project timeTracking | Time Entry.isActive=true; ไม่กรอง deletedAt ของ Client/Project/Task | ยังรวมประวัติบน Client/Project/Task ที่ archive หรือ soft delete; entry ที่ soft delete ปกติมี isActive=false จึงไม่รวม |
| ClientService.summarizeTimeByClient | Time Entry/Project ต้อง active และ deletedAt=null; Task ไม่มีหรือ active และ deletedAt=null; ไม่กรอง Client activation/deletion | ไม่รวมเวลา Project ที่ archive หรือ Task ที่ soft delete แต่ยังอาจคืนกลุ่ม Client ที่ soft delete |
| Dashboard KPI เวลาและ daily/activity | Time Entry active; Project active; Task ไม่มีหรือ active; projection ไม่อ่าน deletedAt/Client | ไม่รวมเวลา Project ที่ archive/Task ที่ soft delete ตาม flow ปกติ แต่ไม่ใช่ query ตรวจ deletedAt ทุกความสัมพันธ์โดยตรง |
| Dashboard ยอดเวลาเทียบเป้าหมายและ recent entries | Time Entry active; Project active และ deletedAt=null; Task ไม่มีหรือ active; ไม่กรอง Client.deletedAt | ไม่รวมเวลา Project ที่ archive; ประวัติ Task ที่ soft delete ปกติไม่รวม; recent คืนไม่เกิน 2 รายการ |
| Reports summary/distribution/projects/work-trend/work-pattern | Time Entry active และ deletedAt=null; Project/Client deletedAt=null; ไม่กรอง Project/Client.isActive หรือ Task; กรอง p.status เมื่อระบุ status | รวม Client/Project ที่ archive และประวัติ Task ที่ soft delete แต่ไม่รวม Project/Client ที่ soft delete |

หลักฐาน: `repository/ClientRepository.java`, `repository/TimeEntryRepository.java`, `repository/ReportQueryRepository.java`, `service/impl/TimeEntryServiceImpl.java` และ `service/impl/DashboardServiceImpl.java` หากเทียบตัวเลขข้ามหน้าต้องใช้ชุดเงื่อนไขเดียวกันก่อน ไม่ถือว่ายอดต่างกันเป็นความผิดของ Frontend เสมอ

**ช่วงเวลาและการจัดกลุ่ม:** Time Entry API/Client internal summary ใช้ Instant แบบ `[from,to)` ส่วน Dashboard/Reports แปลงวันด้วย Asia/Bangkok แล้วกรอง startedAt แบบขอบบนไม่รวม เวลาทั้งรายการถูกลงในวัน/ชั่วโมงที่เริ่ม ไม่ได้แบ่ง duration ข้ามเที่ยงคืนหรือหลายชั่วโมง และไม่ตัด duration ให้เหลือเฉพาะส่วนที่ทับซ้อนช่วงที่เลือก

**การเทียบช่วงก่อน:** Dashboard weekTrackedSeconds ใช้วันจันทร์ถึงสิ้นวันนี้ (toExclusive=พรุ่งนี้) เทียบกับสัปดาห์ก่อนครบจันทร์–อาทิตย์ ไม่ใช่เทียบจำนวนวันที่ผ่านไปเท่ากัน; Reports เทียบช่วงก่อนหน้าที่ติดกันและยาวเท่าช่วงวันที่เลือก สูตร `(current-previous)/previous*100`; Dashboard คืน null หาก previous=0 ส่วน Reports คืน 0 เมื่อทั้งสองช่วงเป็น 0 หรือ 100 เมื่อ current>0 และ previous=0; เมื่อไม่ส่งวันที่ Reports trend เป็น null

### Sequence: โหลด Reports และเปลี่ยน filter

ดู [Sequence 05: Reports และตัวกรอง](diagrams/sequence-diagram.md#scenario-05-load-reports-and-change-filters)

### ขอบเขตที่ยังไม่ครบตาม requirement

- หน้า Dashboard ปัจจุบันเน้น KPI สัปดาห์นี้ ยังไม่มี KPI แยก “วันนี้” และ “เดือนนี้” ตาม `FR-ANA-01` และไม่มีปุ่มเริ่ม/กลับมาจับเวลาบน Dashboard โดยตรง
- `GET /api/reports/work-trend` และ `/work-pattern` มีใน Backend และ service ฝั่ง Frontend แต่หน้า Reports ปัจจุบันยังไม่แสดงกราฟแนวโน้ม ค่าเฉลี่ยรายวัน วัน/ช่วงเวลาที่ทำงานมากที่สุด หรือ productivity trend ทุกมิติตาม `FR-ANA-05`/`FR-ANA-07`
- CSV ปัจจุบันส่งออกตารางโปรเจกต์เฉพาะหน้าที่เห็น ไม่ใช่รายงาน Time Entry ทุกแถวตาม `FR-ANA-08`
- งานที่ต้องทำใน Dashboard ยังไม่แสดง due date; ข้อมูล `OpenTaskResponse` ปัจจุบันไม่มีฟิลด์นี้
- Dashboard summary ยังไม่มี completedProjectCount หรือรายการใกล้เกินเป้าหมายตาม FR-ANA-04 โดยตรง; method countActiveAndCompleted/getProgress มีใน Project service แต่ไม่ใช่หลักฐานว่า Dashboard response/UI ส่งข้อมูลนี้แล้ว

**หลักฐานการทดสอบ:** `code/Frontend/src/lib/dashboard.test.ts`, `dashboard.test.tsx`, `code/Frontend/src/services/report.test.ts`, `code/Backend/src/test/java/th/ac/kku/freelance_hub/integration/DashboardIntegrationTest.java` และ `ReportIntegrationTest.java`

## 8. Diagram และผู้รับผิดชอบ

Sequence และ Activity จากเอกสารสมาชิกถูกรวมไว้ใน [Diagram index](diagrams/README.md) และเพิ่ม [Use Case Diagram](diagrams/use-case-diagram.md), [Domain Model](diagrams/domain-model.md), [State Diagram](diagrams/state-diagram.md) และ [Deployment Diagram](diagrams/deployment-diagram.md) จาก source ณ ca77d74 แล้ว ใช้ actors/IDs และกฎของ implementation ไม่ถือว่าการเพิ่ม diagram implement requirement ที่ยังขาด

| Feature | ผู้รับผิดชอบ |
|---|---|
| Authentication/Profile | Petpinyo |
| Client | Thirawat |
| Project/Task | Kantavit |
| Time Tracking | Kompat |
| Dashboard/Reports | Nattadol |

ฉบับรวมคงความหมายและ requirement boundaries ของสมาชิก ไม่ถือว่าการจัดทำเอกสารเป็นการ implement requirement ที่ยังไม่เสร็จ และไม่ใช้ผล test ในเอกสารเก่าแทน Test Report ของ release ปัจจุบัน
