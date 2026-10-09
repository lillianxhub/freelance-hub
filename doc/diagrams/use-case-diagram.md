# Use Case Diagram - Freelance Hub

ตรวจจาก controllers และ [Use Case Description](../use-case-description.md) ณ `ca77d74` วันที่ 9 ตุลาคม 2026 แบ่งภาพตาม feature เพื่อให้อ่านได้ โดยทุกภาพอยู่ในขอบเขตระบบเดียวกัน

รูปแบบใช้ Mermaid flowchart แทน notation ของ Use Case: กล่อง «actor» อยู่นอก system boundary, รูปวงรีเป็น use case และเส้นทึบเป็น association ไม่ใช่ลำดับการเรียก API รายละเอียด preconditions/alternative flows อยู่ใน Use Case Description

## Actors และขอบเขต

- Guest: ผู้ใช้ที่ยังไม่เข้าสู่ระบบ สมัครบัญชีหรือ login
- Authenticated Freelancer: อ่าน/แก้ข้อมูลของบัญชีตนเองผ่าน access JWT
- User with refresh cookie: ผู้ใช้ที่มี session cookie สำหรับ refresh/logout รวมกรณี access JWT หมดอายุ เป็น role ของคนเดิม ไม่ใช่ผู้ใช้ประเภทใหม่
- JWT filters, repositories, Clock และ event listeners อยู่ภายในระบบ ไม่ใช่ actor ภายนอก
- มี UserRole.ADMIN ใน model แต่ยังไม่มี Admin management endpoint จึงไม่วาด actor/use case ที่ยังไม่ implement

## Authentication และ Profile

```mermaid
flowchart LR
    G["«actor»<br/>Guest"]
    F["«actor»<br/>Authenticated Freelancer"]
    S["«actor»<br/>User with refresh cookie"]
    subgraph AUTH["Freelance Hub: Authentication / Profile"]
        A1(["UC-AUTH-01 สมัครบัญชี"])
        A2(["UC-AUTH-02 เข้าสู่ระบบ"])
        A3(["UC-AUTH-03 ออกจากระบบ"])
        A4(["UC-AUTH-04 Refresh session"])
        U1(["UC-USER-01 ดูโปรไฟล์ตนเอง"])
        U2(["UC-USER-02 แก้โปรไฟล์ตนเอง"])
        U3(["UC-USER-03 เปลี่ยนรหัสผ่าน"])
    end
    G --- A1
    G --- A2
    S --- A3
    S --- A4
    F --- U1
    F --- U2
    F --- U3
```

Logout เป็น public route ที่อ่าน refresh cookie และตรวจ Origin/Referer ไม่จำเป็นต้องมี access JWT ที่ยังไม่หมดอายุ ไม่มี forgot/reset password หรือ GET /api/users/{id} ใน implementation นี้

## Client Management

```mermaid
flowchart LR
    F["«actor»<br/>Authenticated Freelancer"]
    subgraph CLI["Freelance Hub: Client Management"]
        C1(["UC-CLI-01 สร้างลูกค้า"])
        C2(["UC-CLI-02 ค้นหาและดูรายชื่อลูกค้า"])
        C3(["UC-CLI-03 ดูรายละเอียดลูกค้า"])
        C4(["UC-CLI-04 แทนที่ข้อมูลลูกค้า PUT"])
        C5(["UC-CLI-05 แก้ข้อมูลบางส่วน PATCH"])
        C6(["UC-CLI-06 เปลี่ยนสถานะลูกค้า"])
        C7(["UC-CLI-07 Soft-delete ลูกค้า"])
    end
    F --- C1
    F --- C2
    F --- C3
    F --- C4
    F --- C5
    F --- C6
    F --- C7
```

UC-CLI-03 เลือกแนบ Project/Task ด้วย include ได้ แต่ไม่ถือว่าเป็นการแก้ Project/Task; UC-CLI-06 archive Project ที่ผูกอยู่ด้วย ส่วน UC-CLI-07 ตั้ง deletedAt ของ Client อย่างเดียว ทั้งสองเป็นคนละ use case ไม่ใช่ hard delete

## Project และ Task Management

```mermaid
flowchart LR
    F["«actor»<br/>Authenticated Freelancer"]
    subgraph WORK["Freelance Hub: Project / Task Management"]
        subgraph PROJECT["Project"]
            P1(["UC-PRJ-01 สร้าง Project"])
            P2(["UC-PRJ-02 ค้นหาและดูรายการ Project"])
            P3(["UC-PRJ-03 ดู Project รายตัว"])
            P4(["UC-PRJ-04 แก้รายละเอียด Project"])
            P5(["UC-PRJ-05 เปลี่ยนสถานะ Project"])
            P6(["UC-PRJ-06 Soft-delete Project"])
        end
        subgraph TASK["Task"]
            T1(["UC-TSK-01 สร้าง Task"])
            T2(["UC-TSK-02 ดูรายการ Task ใน Project"])
            T3(["UC-TSK-03 ดู Task รายตัว"])
            T4(["UC-TSK-04 แก้ Task"])
            T5(["UC-TSK-05 เปลี่ยนสถานะ Task"])
            T6(["UC-TSK-06 เรียงลำดับ Task"])
            T7(["UC-TSK-07 Soft-delete Task"])
        end
    end
    F --- P1
    F --- P2
    F --- P3
    F --- P4
    F --- P5
    F --- P6
    F --- T1
    F --- T2
    F --- T3
    F --- T4
    F --- T5
    F --- T6
    F --- T7
```

การตรวจ owner/สถานะ Project เป็น precondition ของ Task use cases ไม่ใช่ actor แยก Project เปลี่ยนเป็น COMPLETED จะตรวจ Task ที่ยังใช้งานและ lock รายการเวลาใน transaction เดียวกัน; คำสั่ง lock ไม่เปิดเป็น endpoint ให้ผู้ใช้เรียกเอง

## Timer และ Time Entry

```mermaid
flowchart LR
    F["«actor»<br/>Authenticated Freelancer"]
    subgraph TIME["Freelance Hub: Time Tracking"]
        R1(["UC-TIME-01 เริ่ม Timer"])
        R2(["UC-TIME-02 ดู Timer ปัจจุบัน"])
        R3(["UC-TIME-03 หยุด Timer"])
        R4(["UC-TIME-04 ยกเลิก Timer"])
        E1(["UC-TIME-05 บันทึกเวลาย้อนหลัง"])
        E2(["UC-TIME-06 ดูและกรองรายการเวลา"])
        E3(["UC-TIME-07 สรุปเวลาที่บันทึก"])
        E4(["UC-TIME-08 แก้รายการเวลา"])
        E5(["UC-TIME-09 Soft-delete รายการเวลา"])
        E6(["UC-TIME-10 ดูรายละเอียดรายการเวลา"])
    end
    F --- R1
    F --- R2
    F --- R3
    F --- R4
    F --- E1
    F --- E2
    F --- E3
    F --- E4
    F --- E5
    F --- E6
```

UC-TIME-11 lockByProject เป็น operation ภายในจาก Project completion จึงไม่เชื่อม actor โดยตรง หลังหยุด Timer มี threshold event/log แต่ยังไม่มี use case รับ notification จริงหรือคัดลอก Time Entry เดิม

## Dashboard และ Reports

```mermaid
flowchart LR
    F["«actor»<br/>Authenticated Freelancer"]
    subgraph ANALYTICS["Freelance Hub: Dashboard / Reports"]
        D1(["UC-ANA-01 เปิด Dashboard"])
        D2(["UC-ANA-02 เปลี่ยนช่วงกราฟ Dashboard"])
        D3(["UC-ANA-03 หยุด Timer จาก Dashboard"])
        A1(["UC-ANA-04 เปิด Reports"])
        A2(["UC-ANA-05 กรองและจัดกลุ่ม Reports"])
        A3(["UC-ANA-06 เปลี่ยนหน้าตาราง Reports"])
        A4(["UC-ANA-07 ส่งออก CSV ของหน้าปัจจุบัน"])
        A5(["UC-ANA-08 อ่าน Work Trend ผ่าน API"])
        A6(["UC-ANA-09 อ่าน Work Pattern ผ่าน API"])
    end
    F --- D1
    F --- D2
    F --- D3
    F --- A1
    F --- A2
    F --- A3
    F --- A4
    F --- A5
    F --- A6
```

UC-ANA-03 ใช้ Timer API เดียวกับ UC-TIME-03 ไม่ใช่ timer lifecycle อีกชุด; CSV ทำใน browser ไม่มี endpoint export เพิ่ม UC-ANA-08/09 มี API แต่หน้า Reports ยังไม่มี UI สองส่วนนี้

## วิธีเทียบกับ requirement

Use case IDs ตรงกับ Use Case Description และภาพแสดงพฤติกรรมที่มีจริง ไม่ถือว่าทุก requirement สำเร็จจากการวาดภาพ เช่น Client detail ยังขาดเวลาราย Project, Dashboard บาง KPI ยังไม่ครบ, CSV ยังเป็นตาราง Project ของหน้าปัจจุบัน และ notification ยังเป็น log เท่านั้น

ดู [Domain Model](domain-model.md), [State Diagram](state-diagram.md) และ [Sequence Diagrams](README.md) เพื่อเทียบความสัมพันธ์ กฎสถานะ และลำดับการทำงาน
