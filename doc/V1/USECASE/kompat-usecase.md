# Use Case: Time Tracking

**เจ้าของ feature:** `kompat_673380262-4_02`  
**อ้างอิง requirement:** `FR-TIME-01` ถึง `FR-TIME-09` และ `BR-01` ถึง `BR-08` ใน `REQUIREMENTS.md`

## Actor และเงื่อนไขร่วม

**Actor หลัก:** Freelancer ที่เข้าสู่ระบบด้วย JWT  
**Precondition ร่วม:** Request มี bearer token ที่ถูกต้อง  
**กติกาการเป็นเจ้าของ:** ระบบดึง owner ID จาก authenticated user ไม่รับ owner ID จาก request และค้นหา Project, Task หรือ Time Entry ภายใต้ owner คนนั้นเสมอ

## Use Case Summary

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

## UC-TIME-01 Start Timer

1. Freelancer ส่ง `projectId`, optional `taskId` และ optional `description`
2. Controller validate `StartTimerRequest` และอ่าน owner ID จากผู้ใช้ที่เข้าสู่ระบบ
3. Service ตรวจว่า User และ Project มีอยู่จริง โดย Project ต้องเป็นของ owner
4. หากส่ง Task ระบบตรวจว่า Task อยู่ใน Project และเป็นของ owner คนเดียวกัน
5. Entity ตรวจว่า Project สามารถจับเวลาได้ (`canTrackTime()`) และ Client มี `isActive = true` โดยใช้เวลาจาก server ผ่าน `Clock`
6. Service ตรวจว่า owner ยังไม่มี running timer แล้วบันทึกรายการชนิด `TIMER`; หากมี Task จะเรียก `Task.start()` โดย `OPEN` เปลี่ยนเป็น `IN_PROGRESS`, `IN_PROGRESS` คงเดิม และ `COMPLETED` ถูกปฏิเสธ การบันทึก Timer และการเปลี่ยนสถานะ Task อยู่ใน transaction เดียวกัน
7. Controller คืน `201 Created`, `ApiResult<StartedTimerResponse>` และ `Location: /api/time-entries/{id}` โดย `data` มี `project` และ `task` พร้อม `status` ของแต่ละรายการ

**Alternative flow:** ไม่มี JWT = `401`; Project/Task ไม่พบหรือไม่ใช่ของ owner = `404`; Project ไม่สามารถจับเวลาได้หรือ Client มี `isActive` ไม่ใช่ `true` = `409`; Task เป็น `COMPLETED` = `409` และ transaction ย้อนกลับ; มี running timer อยู่แล้ว = `409`; request ไม่ถูกต้อง = `400`\
**Postcondition:** มี Time Entry ชนิด `TIMER` ที่มี `startedAt` แต่ยังไม่มี `endedAt` และ `durationSeconds`; owner มี running timer ได้ไม่เกินหนึ่งรายการ; Task ที่ส่งมามีสถานะ `IN_PROGRESS`

## UC-TIME-02 View Current Timer

1. Freelancer เรียก `GET /api/timer/current`
2. Service ค้นหารายการชนิด `TIMER` ของ owner ที่ยังไม่มี `endedAt`
3. Controller แปลงผลเป็น `CurrentTimerResponse` โดยกรณีพบ timer จะได้ `running = true` และข้อมูลใน `timeEntry`
4. Controller คืน `200 OK` ภายใน `ApiResult`

**Alternative flow:** ไม่มี JWT = `401`; ไม่มี running timer = `200` พร้อม `running = false` และ `timeEntry = null`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูล

## UC-TIME-03 Stop Timer

1. Freelancer เรียก `POST /api/timer/stop`
2. Service ค้นหา running timer ของ owner ด้วย pessimistic write lock ภายใน transaction
3. Entity กำหนด `endedAt` จาก server clock และคำนวณ `durationSeconds` เป็นจำนวนวินาทีเต็มโดยไม่ปัดขึ้นเป็นนาที
4. Service เผยแพร่ `TimerStoppedEvent`; หลัง transaction commit แล้ว `TimerStoppedProgressListener` จึงตรวจเกณฑ์ความคืบหน้าโปรเจกต์และเผยแพร่ `ProjectProgressThresholdEvent` หากถึง 80% หรือ 100%
5. Controller คืน `200` พร้อม `StoppedTimerResponse` ใน `ApiResult.data`

**Alternative flow:** ไม่มี JWT = `401`; ไม่มี running timer = `404`  
**Postcondition:** Timer กลายเป็น completed entry และมี `startedAt`, `endedAt` และ `durationSeconds`

## UC-TIME-04 Cancel Timer

1. Freelancer เรียก `DELETE /api/timer/current`
2. Service ค้นหาและล็อก running timer ของ owner ภายใน transaction
3. Repository ลบรายการดังกล่าว
4. Controller คืน `200 OK` พร้อม `ApiResult` ที่มี `data: null`

**Alternative flow:** ไม่มี JWT = `401`; ไม่มี running timer = `404`  
**Postcondition:** Running timer ถูกลบโดยไม่สร้าง completed entry และไม่เผยแพร่ `TimerStoppedEvent`

## UC-TIME-05 Create Manual Time Entry

1. Freelancer ส่ง `projectId`, optional `taskId`, optional `description`, `startedAt` และเลือกส่งอย่างใดอย่างหนึ่งระหว่าง `endedAt` หรือ `durationSeconds`
2. Controller validate ว่ามีวิธีกำหนดเวลาสิ้นสุดเพียงแบบเดียวและเวลาสิ้นสุดอยู่หลังเวลาเริ่ม
3. Service ตรวจ User, Project, Task และ owner relationship
4. Entity สร้างรายการชนิด `MANUAL`; หากส่ง duration ระบบคำนวณ `endedAt` หรือหากส่งช่วงเวลาระบบคำนวณ duration
5. หากส่ง Task จะเรียก `Task.start()` ก่อนบันทึก: `OPEN` เปลี่ยนเป็น `IN_PROGRESS`, `IN_PROGRESS` คงเดิม และ `COMPLETED` ถูกปฏิเสธ; การบันทึก Time Entry และการเปลี่ยนสถานะ Task อยู่ใน transaction เดียวกัน
6. Repository บันทึก แล้ว controller คืน `201 Created` พร้อม `Location` และ `TimeEntryDetailResponse` ใน `ApiResult.data` ซึ่งมี `createdAt`, `updatedAt`, `project.status` และ `task.status` เมื่อมี Task

**Alternative flow:** ไม่มี JWT = `401`; Project/Task ไม่พบหรือไม่ใช่ของ owner = `404`; Task เป็น `COMPLETED` = `409` และไม่บันทึกรายการ; ไม่ส่งหรือส่งทั้ง `endedAt` และ `durationSeconds` = `400`; duration ไม่เป็นบวกหรือช่วงเวลาไม่ถูกต้อง = `400`
**Postcondition:** มี completed manual entry ที่ duration มากกว่า 0; Task ที่ส่งมามีสถานะ `IN_PROGRESS`; การบันทึกย้อนหลังไม่บังคับให้ Project เป็น `ACTIVE`

## UC-TIME-06 List and Filter Time Entries

1. Freelancer เรียก `GET /api/time-entries` พร้อม query parameter ที่ต้องการ ได้แก่ `clientId`, `projectId`, `taskId`, `entryType`, `from`, `to`, `page`, `limit`, `sortBy` และ `direction`
2. `TimeEntryService.list()` เริ่มเงื่อนไขด้วย owner ID และ `isActive = true` เสมอ แล้วจึงเพิ่ม filter ที่ส่งมา
3. ช่วงเวลาใช้ `from` แบบ inclusive และ `to` แบบ exclusive กับ `startedAt`
4. Repository คืน `Page<TimeEntry>` และ mapper แปลงเป็น `Page<TimeEntryResponse>`
5. Controller คืน `200 OK` พร้อม `TimeEntryListItemResponse` ใน `data` ซึ่งมี `description` ของแต่ละรายการ รวมถึง timer ที่หยุดแล้ว และ `{page, limit, total, totalPages}` ใน `meta` โดย `page` เริ่มที่ 1

**Alternative flow:** ไม่มีผลลัพธ์ = `data` เป็นรายการว่าง; `from` ไม่น้อยกว่า `to`, page/limit, sort field หรือ direction ไม่ถูกต้อง = `400`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูลและไม่แสดง Time Entry ของผู้ใช้อื่น

## UC-TIME-07 Summarize Time Entries

1. Freelancer เรียก `GET /api/time-entries/summary` พร้อม filter ชุดเดียวกับรายการ
2. `TimeEntryService.summarize()` จำกัดข้อมูลด้วย owner, `isActive = true` และ filter ที่ระบุ; `from` รวมขอบล่าง ส่วน `to` ไม่รวมขอบบน โดยเทียบกับ `startedAt`
3. ระบบนับเฉพาะรายการที่มี `endedAt` และ `durationSeconds` จึงไม่นับ running timer หรือรายการที่ soft delete
4. ระบบคืน `entryCount`, `totalSeconds`, `from` และ `to` ใน `TimeEntrySummaryResponse` ภายใต้ `ApiResult.data`; `page`, `limit` และการ sort ไม่เปลี่ยนผลรวม

**Alternative flow:** ไม่มีรายการที่เข้าเงื่อนไข = `entryCount` และ `totalSeconds` เป็น `0`; ช่วงเวลาไม่ถูกต้อง = `400`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูลและไม่มีการคำนวณมูลค่าเงิน

## UC-TIME-08 Update Time Entry

1. Freelancer ส่ง UUID ของรายการใน path และข้อมูลทดแทนผ่าน `PUT` ได้แก่ `projectId`, `startedAt`, optional `taskId`/`description` และอย่างใดอย่างหนึ่งระหว่าง `endedAt` หรือ `durationSeconds`
2. Controller validate ข้อมูลที่จำเป็นและเวลาที่ส่งมา; `taskId` ที่ไม่ส่งหรือเป็น `null` จะล้าง Task เดิม และ `description` ที่ไม่ส่งหรือเป็น `null` จะล้างคำอธิบายเดิม
3. Service ค้นหารายการด้วย `entryId` และ `ownerId` แล้วตรวจว่าไม่ถูกล็อก
4. หากเปลี่ยน Project หรือ Task ระบบตรวจ owner และ task-project relationship อีกครั้ง
5. Entity แก้ข้อมูลและคำนวณ `durationSeconds` ใหม่ตามช่วงเวลาหรือค่าที่ส่งมา
6. Controller คืน `200` พร้อม `TimeEntryDetailResponse` ใน `ApiResult.data`

**Alternative flow:** ไม่พบหรือเป็นของผู้ใช้อื่นหรือถูก soft delete = `404`; รายการถูกล็อก = `409`; ช่วงเวลาไม่ถูกต้องหรือ request ไม่ครบ = `400`; พยายามแก้ running timer = `409`; Project/Task ไม่ถูกต้อง = `404`; ไม่มี JWT = `401`
**Postcondition:** ข้อมูลที่ส่งมาแทนค่าเดิม; ไม่ส่ง Task จะล้าง Task เดิม

## UC-TIME-09 Delete Time Entry

1. Freelancer ส่ง UUID ไปที่ `DELETE /api/time-entries/{id}`
2. Service ค้นหารายการด้วย `entryId` และ `ownerId`
3. Service ปฏิเสธรายการที่ถูกล็อกหรือ timer ที่ยังทำงาน
4. Entity กำหนด `isActive = false` และ `deletedAt` จาก server clock แล้วบันทึก; controller คืน `200 OK` พร้อม `data: null`

**Alternative flow:** ไม่พบหรือเป็นของผู้ใช้อื่น = `404`; รายการถูกล็อก = `409`; รายการเป็น running timer = `409` และต้องใช้ cancel timer flow; ไม่มี JWT = `401`  
**Postcondition:** Completed entry ยังอยู่ในฐานข้อมูล แต่ไม่ปรากฏใน list, detail และ summary ปกติ

## UC-TIME-10 View Time Entry Detail

1. Freelancer เรียก `GET /api/time-entries/{id}` พร้อม UUID ของรายการ
2. `TimeEntryService.getById()` ค้นหาเฉพาะรายการที่เป็นของ owner และ `isActive = true`
3. Controller คืน `200 OK` พร้อม `TimeEntryDetailResponse` ใน `ApiResult.data` รวม `createdAt` และ `updatedAt`

**Alternative flow:** ไม่พบ เป็นของผู้ใช้อื่น หรือถูก soft delete = `404`; ไม่มี JWT = `401`
**Postcondition:** ไม่มีการเปลี่ยนข้อมูล

## UC-TIME-11 Lock Project Time Entries

1. `ProjectServiceImpl.changeStatus()` ตรวจสิทธิ์เจ้าของและไม่ให้มี running timer ใน Project ก่อนเปลี่ยนสถานะ; เมื่อเปลี่ยนเป็น `COMPLETED` จะเรียก `TimeEntryService.lockByProject(ownerId, projectId)` ใน transaction เดียวกัน โดยไม่มีคำสั่ง lock จากหน้าบ้าน
2. `TimeEntryServiceImpl` ใช้ `@Transactional(propagation = Propagation.MANDATORY)` เพื่อบังคับว่าผู้เรียกต้องเปิด transaction ไว้แล้ว
3. Repository ใช้ `findLockedByOwnerIdAndProjectIdAndLockedAtIsNull` พร้อม `PESSIMISTIC_WRITE` เพื่อดึงเฉพาะรายการที่ยังไม่ล็อกของ owner/Project ที่ระบุ รวมรายการที่ soft delete และ timer ที่ยังวิ่งอยู่
4. Service ตรวจทุกรายการก่อนแก้ข้อมูล; หากพบ running timer จะโยน `IllegalStateException` โดยไม่หยุด timer อัตโนมัติและไม่ตั้ง `lockedAt` ให้รายการใด
5. หากไม่มี running timer ระบบอ่านเวลาจาก `Clock` ครั้งเดียว แล้วเรียก `TimeEntry.lock(lockedAt)` กับทุกรายการที่พบและ flush ภายใน transaction ของผู้เรียก
6. รายการที่ล็อกอยู่แล้วไม่ถูกแก้และรักษา `lockedAt` เดิม; ไม่มีรายการที่ต้องล็อกสามารถจบการทำงานได้

**Alternative flow:** ไม่มี transaction = `IllegalTransactionStateException`; ไม่ส่ง owner/project ID = `NullPointerException` ก่อน query; พบ running timer = `IllegalStateException` ซึ่งผู้เรียกต้องปล่อยให้ transaction ย้อนกลับ โดย HTTP response เป็นหน้าที่ของ API ฝั่งผู้เรียก\
**Postcondition:** เมื่อ transaction commit รายการที่ถูกเลือกมี `lockedAt` ถาวรและไม่สามารถแก้ไขหรือ soft delete ผ่าน Entity/service ปกติได้; การล็อกแถวฐานข้อมูลสิ้นสุดเมื่อ transaction จบ แต่ค่า `lockedAt` ยังอยู่

**สถานะการเชื่อมต่อ:** `ProjectServiceImpl.changeStatus()` เรียกเมธอดล็อกเมื่อ Project เปลี่ยนเป็น `COMPLETED` แล้ว; integration test ตรวจว่าการเปลี่ยนสถานะผ่าน API ตั้ง `lockedAt` ในฐานข้อมูล และการแก้ไข/ลบ Time Entry หลังจากนั้นได้ `409 TIME_ENTRY_LOCKED` แต่ยังต้องพิจารณาการสร้างหรือย้ายรายการใหม่เข้ามาใน Project ที่ปิดแล้ว

## Sequence: Start และ Stop Timer

```mermaid
sequenceDiagram
    actor F as Freelancer
    participant C as TimerController
    participant U as UserService
    participant S as TimerServiceImpl
    participant P as Project/Task Repository
    participant R as TimeEntryRepository
    participant E as TimeEntry
    participant T as Task
    participant B as ApplicationEventPublisher

    F->>C: POST /api/timer/start + Bearer JWT
    C->>U: getCurrentUserEntity().getId()
    U-->>C: ownerId
    C->>S: startTimer(ownerId, request)
    S->>P: validate owned Project/Task
    S->>E: startTimer(..., Instant.now(clock))
    S->>R: check running timer
    S->>R: saveAndFlush(entry)
    opt ส่ง Task
        S->>T: start() (OPEN เปลี่ยน, IN_PROGRESS คงเดิม, COMPLETED ปฏิเสธ)
        opt สถานะเปลี่ยน
            S->>P: saveAndFlush(task)
        end
    end
    S-->>C: TimeEntryResponse
    C->>C: StartedTimerResponse.from(response)
    C-->>F: 201 Created + ApiResult.data

    F->>C: POST /api/timer/stop + Bearer JWT
    C->>S: stopTimer(ownerId)
    S->>R: find locked running timer
    R-->>S: TimeEntry
    S->>E: stop(Instant.now(clock))
    S->>B: publish TimerStoppedEvent
    S-->>C: TimeEntryResponse
    C-->>F: 200 OK + ApiResult.data
```

## ขอบเขตที่ยังไม่เสร็จ

- `FR-TIME-09` การคัดลอกรายการเดิมเพื่อบันทึกซ้ำยังไม่มี endpoint หรือ service operation
- `GET /api/time-entries/summary` มีอยู่ใน implementation แต่ไม่อยู่ใน API contract ที่ได้รับมา; ยังต้องยืนยันกับทีมว่าจะเก็บ endpoint นี้ไว้หรือไม่
- `FR-TIME-06` รองรับรายวันและรายสัปดาห์ผ่านการส่งขอบเขต `from/to` แต่ยังไม่มี endpoint ที่จัดกลุ่มผลลัพธ์เป็นวันหรือสัปดาห์โดยตรง
- `BR-07` ใช้ `Instant` สำหรับเวลา UTC แต่การแสดงผลตาม timezone ของผู้ใช้เป็นหน้าที่ของ client และยังไม่มี user-timezone conversion ใน Time Tracking API
- มี `TimeEntryService.lockByProject()` สำหรับล็อกถาวรตาม Project รวม soft-deleted แล้ว โดยไม่มี API ให้หน้าบ้านสั่ง lock; ฝั่ง Project เรียกเมธอดนี้เมื่อเปลี่ยนเป็น `COMPLETED` ใน transaction เดียวกันแล้ว แต่ยังต้องจัดการ concurrent creation/reassignment
- เมธอดล็อกครอบคลุมรายการที่มีอยู่ขณะเรียกเท่านั้น; ปัจจุบันยังไม่มีการล็อกอัตโนมัติสำหรับ manual entry ที่สร้างใหม่หรือรายการที่ย้ายเข้ามาภายหลังใน Project ที่ `COMPLETED`
- Audit event สำหรับการแก้ไข Time Entry ตาม non-functional requirement ยังไม่ได้แสดงใน implementation นี้

**หลักฐานการทดสอบ:** `TimerControllerTest`, `TimeEntryControllerTest`, `TimerServiceImplTest`, `TimeEntryServiceImplTest` (รวมกลุ่ม `Queries` สำหรับงานอ่าน), `TimeEntryRepositoryTest`, `ProjectServiceImplTest` และ `TimeEntryIntegrationTest` ภายใต้ `code/Backend/src/test/java/th/ac/kku/freelance_hub/`

การล็อกมี 4 unit test cases ใน `TimeEntryServiceImplTest` สำหรับ manual/completed timer/soft-deleted ด้วย server clock, ปฏิเสธ running timer ก่อนล็อก, ไม่มีรายการ และ ID ที่จำเป็น; อีก 2 cases ใน `TimeEntryRepositoryTest` ตรวจการบันทึกจริง การแยก owner/Project การรักษาเวลาล็อกเดิมและเรียกซ้ำ รวมถึงการปฏิเสธเมื่อไม่มี transaction โดยใช้ฐานข้อมูล H2 ใน test profile; `ProjectServiceImplTest` ตรวจจุดเรียกเมื่อเปลี่ยนเป็น `COMPLETED` และ `TimeEntryIntegrationTest` ตรวจ flow ผ่าน API จนถึง `lockedAt` และข้อผิดพลาด `409` เมื่อแก้ไขหรือลบ
