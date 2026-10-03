# SOLID Analysis: Time Tracking

**เจ้าของ feature:** `kompat_673380262-4_02`  
**ขอบเขต:** Timer, Manual Time Entry, การล็อก Time Entry ตาม Project, Time Entry Query, TimerStoppedEvent และ listener ที่ตอบสนองต่อ event

เอกสารนี้อ้างอิง implementation ปัจจุบันภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` โดยอ้างชื่อคลาสและเมธอดแทนเลขบรรทัดที่เปลี่ยนได้เมื่อแก้โค้ด

| Principle | หลักฐานในโค้ด | เหตุผลที่ใช้ |
|---|---|---|
| Single Responsibility | `TimerServiceImpl.startTimer/getCurrentTimer/stopTimer/cancelTimer` | รับผิดชอบวงจรชีวิตของ timer รวมถึงเผยแพร่ event เมื่อหยุด timer |
| Single Responsibility | `TimeEntryServiceImpl` | รวมงานอ่าน สร้าง แก้ ลบ ล็อก และรวมเวลาในขอบเขต Time Entry; controller, mapper และ repository แยกหน้าที่กัน |
| Single Responsibility | `TimerStoppedProgressListener.onTimerStopped` | รับ event หลัง transaction หยุด timer commit แล้ว คำนวณความคืบหน้าโปรเจกต์และเผยแพร่ `ProjectProgressThresholdEvent` เมื่อถึงเกณฑ์ |
| Single Responsibility | `TimerController`, `TimeEntryController`, `TimeTrackingExceptionHandler` | Controller รับ request และสร้าง `ApiResult`; handler แปลง exception ของ Time Tracking เป็น HTTP error โดยไม่ใส่กฎธุรกิจใน controller |
| Single Responsibility | `TimeEntryMapper`, `TimeEntryRepository` | Mapper แปลง entity เป็น response DTO ส่วน Repository กำหนด data-access contract |
| Open/Closed | `TimerServiceImpl.stopTimer`, `TimerStoppedEvent`, `TimerStoppedProgressListener` | `stopTimer()` เผยแพร่ event โดยไม่ต้องฝังการคำนวณความคืบหน้าไว้ในเมธอด; listener ที่มีอยู่รับ event หลัง commit และเผยแพร่ `ProjectProgressThresholdEvent` เมื่อถึง 80% หรือ 100% |
| Liskov Substitution | `TimerService`/`TimerServiceImpl`, `TimeEntryService`/`TimeEntryServiceImpl` | Controller รับ dependency เป็น service interface และเรียกเมธอดตาม contract โดยไม่อ้างถึง implementation โดยตรง จึงสามารถใช้ implementation ที่รักษา contract เดียวกันแทนได้ในเชิงโครงสร้าง; ยังไม่ได้พิสูจน์พฤติกรรมของ implementation หลายตัว |
| Interface Segregation | `TimerService`,`TimeEntryService` | แยกงานของ TimeEntry และ Timer|
| Dependency Inversion | `TimerController`, `TimeEntryController` | Controller พึ่ง service interfaces แทน implementation classes |
| Dependency Inversion | `TimerServiceImpl`, `TimeEntryServiceImpl` | Service implementations รับ repository interfaces และ dependencies ผ่าน constructor |
| Dependency Inversion | `TimeConfiguration.clock`, `TimerServiceImpl`, `TimeEntryServiceImpl` | Service รับ `Clock` จากภายนอกสำหรับเวลาเริ่ม/หยุด timer เวลา soft delete และ `lockedAt`; production ใช้ `Clock.systemUTC()` ส่วน test ใช้ `Clock.fixed()` ได้ |

## Evidence จาก Time Tracking flow

- Owner ID มาจาก authenticated user ผ่าน `currentOwnerId()` ใน controller ไม่ได้รับจาก request body
- การอ่าน แก้ไข และลบใช้ `findByIdAndOwnerIdAndIsActiveTrue` หรือ owner-scoped specification เพื่อไม่ให้เข้าถึงรายการของผู้ใช้อื่นหรือรายการที่ soft delete
- การหยุดและยกเลิก timer ใช้ locked query แบบ pessimistic write ภายใน transaction; การยกเลิกลบ running timer จริง ส่วนการลบ completed entry ใช้ `TimeEntry.softDelete()`
- Entity ตรวจ Project/Client สำหรับการเริ่ม timer และคำนวณ `durationSeconds` เป็นวินาทีเต็ม โดยไม่ปัดขึ้นเป็นนาที
- เวลาเริ่ม/หยุด timer เวลา soft delete และเวลาล็อกมาจาก `Instant.now(clock)` ใน service; `lockByProject` ใช้เวลาเดียวกันสำหรับทุกรายการในชุด
- `TimeEntryService.lockByProject` เป็นคำสั่งภายใน Backend ใช้ transaction ของผู้เรียกผ่าน `Propagation.MANDATORY`; repository ดึงเฉพาะ owner/Project ที่ระบุและ `lockedAt IS NULL` ด้วย pessimistic write lock รวม soft-deleted โดยรักษาเวลาล็อกเดิมและปฏิเสธ running timer ก่อนแก้ข้อมูล
- เมธอดอ่านใน `TimeEntryServiceImpl` ใช้ `@Transactional(readOnly = true)` รายเมธอด; เมธอดเขียนใช้ transaction สำหรับการเปลี่ยนข้อมูลตามเดิม list และ summary กรอง `isActive = true` ของ Time Entry โดย summary ไม่นับ running timer ส่วนการรวมเวลาสำหรับ analytics ไม่นับ Project/Task ที่ `isActive = false` ด้วย
- `TimeEntryController`, Dashboard, Project, Task และ `TimerStoppedProgressListener` เรียกผ่าน `TimeEntryService` เดียว; unit tests งานอ่านอยู่ในกลุ่ม `Queries` แบบ `@Nested` ของ `TimeEntryServiceImplTest`
- HTTP success ใช้ `ApiResult` ส่วนกลาง ส่วน `TimeTrackingExceptionHandler` แปลง error เฉพาะ Timer และ Time Entry เป็น envelope เดียวกัน

## ข้อจำกัดและข้อสังเกต

1. หลักฐาน OCP เป็นจุดต่อขยายผ่าน event เท่านั้น; การเพิ่มเงื่อนไข filter ใหม่ยังต้องแก้ `buildSpecification()` ใน `TimeEntryServiceImpl`
2. หลักฐาน LSP ข้างต้นแสดงเพียงการเรียกผ่าน interface และการ implement contract ปัจจุบัน; แต่ละ service มี production implementation เพียงตัวเดียว จึงยังยืนยันไม่ได้ว่า implementation ใหม่จะทดแทนกันได้โดยไม่เปลี่ยนพฤติกรรม เช่น การตรวจ owner, ข้อผิดพลาด และผลลัพธ์ที่คืน
3. `TimeTrackingExceptionHandler` ครอบคลุมเฉพาะ Timer และ Time Entry; authentication error และ URL ที่ไม่ตรง endpoint ยังผ่านกลไกส่วนกลางของ Backend
4. `ProjectServiceImpl.changeStatus()` เรียก `lockByProject` เมื่อเปลี่ยนเป็น `COMPLETED` แล้ว และมี integration test ยืนยันการตั้ง `lockedAt` กับการปฏิเสธ update/delete; เมธอดล็อกไม่ได้ตรวจสถานะ Project เอง และการล็อกเฉพาะรายการที่มีอยู่ยังไม่ครอบคลุม concurrent creation/reassignment
