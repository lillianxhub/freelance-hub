# SOLID Analysis: Time Tracking

**เจ้าของ feature:** `kompat_673380262-4_02`  
**ขอบเขต:** Timer, Manual Time Entry, Time Entry Query และ TimerStoppedEvent

เอกสารนี้อ้างอิง implementation ปัจจุบันภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` โดยระบุเฉพาะหลักฐานที่พบในโค้ดจริง เลขบรรทัดควรตรวจซ้ำหลัง code freeze

| Principle | ไฟล์/คลาสและบรรทัด | เหตุผลที่ใช้ |
|---|---|---|
| Single Responsibility | `service/impl/TimerServiceImpl.java:35-195` | รับผิดชอบวงจรชีวิตของ timer ได้แก่ start, current, stop และ cancel รวมถึงเผยแพร่ event เมื่อหยุด timer |
| Single Responsibility | `service/impl/TimeEntryServiceImpl.java:30-237` | รับผิดชอบคำสั่งของ completed entry ได้แก่ create manual, update และ delete |
| Single Responsibility | `service/impl/TimeEntryQueryServiceImpl.java:27-206` | รับผิดชอบงานอ่าน ได้แก่ list, filtering, pagination และ summary โดยใช้ transaction แบบ read-only |
| Single Responsibility | `controller/TimerController.java:33-112`, `controller/TimeEntryController.java:44-149` | Controller รับ HTTP request, ดึง owner จากผู้ใช้ที่ยืนยันตัวตนแล้ว เรียก service และสร้าง HTTP response โดยไม่ใส่ business logic |
| Single Responsibility | `mapper/TimeEntryMapper.java:15-45`, `repository/TimeEntryRepository.java:24-53` | Mapper ดูแลการแปลง DTO ส่วน Repository ดูแล data-access contract |
| Open/Closed | `service/impl/TimerServiceImpl.java:123-142`, `event/TimerStoppedEvent.java:8-35` | เมื่อหยุด timer ระบบเผยแพร่ event ทำให้เพิ่ม listener สำหรับ analytics, notification หรือ logging ได้โดยไม่แก้ `stopTimer()` |
| Liskov Substitution | `service/TimerService.java:9-18`, `service/impl/TimerServiceImpl.java:35-195` | `TimerServiceImpl` ทำตาม contract และถูกใช้งานผ่าน `TimerService` |
| Liskov Substitution | `service/TimeEntryService.java:15-33`, `service/TimeEntryQueryService.java:12-23` | Implementation คืนชนิดข้อมูลและรักษาพฤติกรรมตาม service contract |
| Interface Segregation | `service/TimerService.java:9-18` | มีเฉพาะ operation ของ timer ที่ `TimerController` ใช้จริง |
| Interface Segregation | `service/TimeEntryService.java:15-33` | มีเฉพาะคำสั่ง create manual, update และ delete ไม่บังคับให้ผู้ใช้พึ่ง query methods |
| Interface Segregation | `service/TimeEntryQueryService.java:12-23` | แยก list และ summary ออกจาก mutation methods ทำให้ผู้ใช้งานฝั่ง query พึ่งเฉพาะสิ่งที่จำเป็น |
| Dependency Inversion | `controller/TimerController.java:35`, `controller/TimeEntryController.java:46-47` | Controller พึ่ง service interfaces แทน implementation classes |
| Dependency Inversion | `service/impl/TimerServiceImpl.java:39-64`, `service/impl/TimeEntryServiceImpl.java:32-50`, `service/impl/TimeEntryQueryServiceImpl.java:36-46` | Service implementations รับ dependencies ผ่าน constructor และพึ่ง repository interfaces แทนการสร้าง dependencies เอง |
| Dependency Inversion | `config/TimeConfiguration.java:10-15`, `service/impl/TimerServiceImpl.java:45-55` | `TimerServiceImpl` รับ `Clock` จากภายนอกแทนการอ่านเวลาระบบโดยตรง ทำให้ระบบจริงใช้เวลาปัจจุบันจาก `Clock.systemUTC()` ส่วน test สามารถกำหนดเวลาตายตัวด้วย `Clock.fixed()` ได้ |

## Evidence จาก Time Tracking flow

- Owner ID มาจาก authenticated user ใน controller ไม่ได้รับจาก request body: `TimerController.java:57-63,80-83,91-94,106-112`, `TimeEntryController.java:68-75,85-102,119-148`
- การอ่าน แก้ไข และลบ Time Entry ใช้ owner-scoped query: `TimeEntryServiceImpl.java:163-180`, `TimeEntryRepository.java:28`
- การหยุดและยกเลิก timer ใช้ pessimistic lock ภายใน transaction: `TimerServiceImpl.java:123-157`, `TimeEntryRepository.java:39-53`
- Entity รักษากฎการเริ่มและหยุด timer รวมถึงคำนวณ duration: `TimeEntry.java:118-199,243-253`
- เวลาเริ่มและเวลาหยุดมาจาก server ผ่าน `Instant.now(clock)`: `TimerServiceImpl.java:84,128`
- Query service ใช้ `@Transactional(readOnly = true)`: `TimeEntryQueryServiceImpl.java:26-27`

## ข้อจำกัดและข้อสังเกต

1. หลักฐาน OCP ชัดเจนเฉพาะ event extension; การเพิ่มเงื่อนไข filter ใหม่ยังต้องแก้ `buildSpecification()` ใน `TimeEntryQueryServiceImpl.java:103-171`
2. แต่ละ service interface มี production implementation เพียงตัวเดียว จึงยังไม่สามารถพิสูจน์ LSP ด้วยการสลับหลาย implementation ได้
3. ต้องตรวจเลขบรรทัดอีกครั้งหลัง code freeze ก่อนรวมเนื้อหาเข้า `doc/solid-analysis.md`
