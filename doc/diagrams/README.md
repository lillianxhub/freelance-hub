# Diagram Index - Freelance Hub

รวม diagram ที่มีต้นฉบับในเอกสารสมาชิก และตรวจชื่อ class/method/response กับ implementation ณ commit `131305f` วันที่ 9 ตุลาคม 2026 โดยไม่เปลี่ยนไฟล์ V1

## Diagram ที่จัดไว้แล้ว

| ประเภท | ไฟล์ | ที่มา / ขอบเขต |
|---|---|---|
| Domain Class Model | [Class Diagram](class-diagram.md) | Domain model เดิม ตรวจ Entity methods และ embedded Address แล้ว |
| Class Diagram + Patterns | [Application/Authentication/State](class-diagram.md#application-layers) | รวมจาก diagrams ของสมาชิกและแสดง pattern placement |
| ER / Database Schema | [ER Diagram](er-diagram.md) | Schema หลัง migrations V1-V20; Data Dictionary เป็นรายละเอียด constraints |
| Sequence 01 | [Login และ protected request](sequence-01-auth.md) | Petpinyo |
| Sequence 02 | [Start และ Stop Timer](sequence-02-timer.md) | Kompat |
| Sequence 03 | [เปลี่ยนสถานะ Project](sequence-03-project-status.md) | Kantavit |
| Sequence 04 | [Soft-delete Client](sequence-04-client.md) | Thirawat |
| Sequence 05 | [Reports และตัวกรอง](sequence-05-reports.md) | Nattadol |
| Sequence 06 | [Progress Events](sequence-06-progress-events.md) | Kantavit / Time Tracking events |
| Activity | [Timer](activity-timer.md), [Project status](activity-project-status.md) | Kompat และ Kantavit |
| Component view | [Dashboard/Reports](component.md) | Nattadol; ขอบเขต Frontend/API/Database ไม่ใช่ full Deployment Diagram |

Sequence มี 6 scenarios จากต้นฉบับที่มี ครบจำนวนขั้นต่ำ 3 scenarios ในใบงาน โดยต้องตรวจ syntax/ภาพที่ renderer ของการส่งงานรองรับอีกครั้งเมื่อมีการแก้ diagram

## ส่วนที่ยังไม่มีต้นฉบับแยก จึงยังไม่สร้าง

- Use Case Diagram (มี [Use Case Description](../use-case-description.md) แล้ว)
- Conceptual Domain Model แบบแยกจาก detailed Entity class model
- Deployment Diagram ระบุ cloud/runtime nodes และ connections
- State Diagram ของ Project หรือ Entity ที่มีสถานะ (มี transition table และ State pattern class diagram แต่ยังไม่ใช่ State Diagram)

ไม่ใช้ Invoice State Diagram เป็นรายการส่งของ MVP เพราะ Finance/Invoice อยู่นอกขอบเขตระบบปัจจุบัน

## การเปิดดูและส่งมอบ

Source diagram เป็น Mermaid ใน Markdown เปิดไฟล์ใน GitHub แบบ Preview เพื่อดูภาพ และเก็บ source ไว้คู่กับภาพที่ export เมื่อทีมจัดชุดส่งมอบ ไฟล์ภาพ export ยังไม่ถูกสร้างในรอบนี้

อ้างอิงภาพรวมที่ [Design Patterns](../design-patterns.md), [Data Dictionary](../data-dictionary.md) และ [Use Case Description](../use-case-description.md)

