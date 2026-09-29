# SOLID Analysis: Time Tracking

**เจ้าของ feature:** `kompat_673380262-4_02`  
**ขอบเขต:** Timer, Manual Time Entry, Time Entry Query และ TimerStoppedEvent

เอกสารนี้อ้างอิง implementation ปัจจุบันภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` โดยอ้างชื่อคลาสและเมธอดแทนเลขบรรทัดที่เปลี่ยนได้เมื่อแก้โค้ด

| Principle | หลักฐานในโค้ด | เหตุผลที่ใช้ |
|---|---|---|
| Single Responsibility | `TimerServiceImpl.startTimer/getCurrentTimer/stopTimer/cancelTimer` | รับผิดชอบวงจรชีวิตของ timer รวมถึงเผยแพร่ event เมื่อหยุด timer |
| Single Responsibility | `TimeEntryServiceImpl.createManual/update/delete` | รับผิดชอบคำสั่งของ completed entry; `delete` ทำ soft delete |
| Single Responsibility | `TimeEntryQueryServiceImpl.getById/list/summarize` | รับผิดชอบงานอ่าน การกรอง และ pagination โดยใช้ transaction แบบ read-only |
| Single Responsibility | `TimerController`, `TimeEntryController`, `TimeTrackingExceptionHandler` | Controller รับ request และสร้าง `ApiResult`; handler แปลง exception ของ Time Tracking เป็น HTTP error โดยไม่ใส่กฎธุรกิจใน controller |
| Single Responsibility | `TimeEntryMapper`, `TimeEntryRepository` | Mapper แปลง entity เป็น response DTO ส่วน Repository กำหนด data-access contract |
| Open/Closed | `TimerServiceImpl.stopTimer`, `TimerStoppedEvent` | `stopTimer()` เผยแพร่ event เป็นจุดต่อขยายสำหรับ listener ในอนาคต โดยไม่ต้องเพิ่มงาน analytics หรือ notification ในเมธอดนี้; ยังไม่มี listener ในขอบเขต Time Tracking นี้ |
| Liskov Substitution | `TimerService`/`TimerServiceImpl`, `TimeEntryService`/`TimeEntryServiceImpl`, `TimeEntryQueryService`/`TimeEntryQueryServiceImpl` | Controller รับ dependency เป็น service interface และเรียกเมธอดตาม contract โดยไม่อ้างถึง implementation โดยตรง จึงสามารถใช้ implementation ที่รักษา contract เดียวกันแทนได้ในเชิงโครงสร้าง; ยังไม่ได้พิสูจน์พฤติกรรมของ implementation หลายตัว |
| Interface Segregation | `TimerService` | มีเฉพาะ operation ของ timer ที่ `TimerController` ใช้ |
| Interface Segregation | `TimeEntryService` | มีเฉพาะคำสั่ง create manual, update และ delete ไม่บังคับให้ controller พึ่ง query methods |
| Interface Segregation | `TimeEntryQueryService` | แยก getById, list และ summary ออกจาก mutation methods |
| Dependency Inversion | `TimerController`, `TimeEntryController` | Controller พึ่ง service interfaces แทน implementation classes |
| Dependency Inversion | `TimerServiceImpl`, `TimeEntryServiceImpl`, `TimeEntryQueryServiceImpl` | Service implementations รับ repository interfaces และ dependencies ผ่าน constructor |
| Dependency Inversion | `TimeConfiguration.clock`, `TimerServiceImpl`, `TimeEntryServiceImpl` | Service รับ `Clock` จากภายนอกสำหรับเวลาเริ่ม/หยุด timer และเวลา soft delete; production ใช้ `Clock.systemUTC()` ส่วน test ใช้ `Clock.fixed()` ได้ |

## Evidence จาก Time Tracking flow

- Owner ID มาจาก authenticated user ผ่าน `currentOwnerId()` ใน controller ไม่ได้รับจาก request body
- การอ่าน แก้ไข และลบใช้ `findByIdAndOwnerIdAndIsActiveTrue` หรือ owner-scoped specification เพื่อไม่ให้เข้าถึงรายการของผู้ใช้อื่นหรือรายการที่ soft delete
- การหยุดและยกเลิก timer ใช้ locked query แบบ pessimistic write ภายใน transaction; การยกเลิกลบ running timer จริง ส่วนการลบ completed entry ใช้ `TimeEntry.softDelete()`
- Entity ตรวจ Project/Client สำหรับการเริ่ม timer และคำนวณ `durationSeconds` เป็นวินาทีเต็ม โดยไม่ปัดขึ้นเป็นนาที
- เวลาเริ่ม/หยุด timer และเวลา soft delete มาจาก `Instant.now(clock)` ใน service
- Query service ใช้ `@Transactional(readOnly = true)` และกรองเฉพาะ `isActive = true`; summary ไม่นับ running timer
- HTTP success ใช้ `ApiResult` ส่วนกลาง ส่วน `TimeTrackingExceptionHandler` แปลง error เฉพาะ Timer และ Time Entry เป็น envelope เดียวกัน

## ข้อจำกัดและข้อสังเกต

1. หลักฐาน OCP เป็นจุดต่อขยายผ่าน event เท่านั้น; การเพิ่มเงื่อนไข filter ใหม่ยังต้องแก้ `buildSpecification()` ใน `TimeEntryQueryServiceImpl`
2. หลักฐาน LSP ข้างต้นแสดงเพียงการเรียกผ่าน interface และการ implement contract ปัจจุบัน; แต่ละ service มี production implementation เพียงตัวเดียว จึงยังยืนยันไม่ได้ว่า implementation ใหม่จะทดแทนกันได้โดยไม่เปลี่ยนพฤติกรรม เช่น การตรวจ owner, ข้อผิดพลาด และผลลัพธ์ที่คืน
3. `TimeTrackingExceptionHandler` ครอบคลุมเฉพาะ Timer และ Time Entry; authentication error และ URL ที่ไม่ตรง endpoint ยังผ่านกลไกส่วนกลางของ Backend
