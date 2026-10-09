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
| Use Case Diagram | [Use Cases แยกตาม feature](use-case-diagram.md) | Actors/system boundary และ IDs ตรงกับฉบับรวม; ไม่สร้าง use case ของ feature ที่ยังไม่ implement |
| Conceptual Domain Model | [Domain Model](domain-model.md) | คำศัพท์ธุรกิจ/ความสัมพันธ์ ไม่ใช่ schema หรือ class members ทุกตัว |
| Deployment Diagram | [Cloud และ Local Nodes](deployment-diagram.md) | Vercel, Render, Supabase, Compose และ deployment channels ตาม configuration |
| State Diagram | [Project/Client/Task/Time Entry](state-diagram.md) | Transitions, guards, activation, soft delete และ locking ตาม implementation |

Sequence มี 6 scenarios จากต้นฉบับที่มี ครบจำนวนขั้นต่ำ 3 scenarios ในใบงาน ตรวจ syntax ด้วย Mermaid 11.12.0 แล้วผ่านทั้ง 29 blocks ใน doc/ และ doc/diagrams/ การตรวจนี้ไม่แทนการตรวจ layout ของภาพที่ renderer ของการส่งงานแสดงจริง

## ขอบเขตของ diagram ที่เพิ่ม

Use Case/Domain/Deployment/State source diagrams ที่เคยขาดเพิ่มจาก source ณ ca77d74 แล้ว ไม่ใช่การคัดลอก diagram ของระบบอื่นมาใช้ ไม่มี Admin/Invoice/Workspace/Notification endpoints ที่ยังไม่ implement และ Deployment nodes เป็น logical view ที่ต้องเทียบ cloud settings/health จริงก่อนส่ง

ไม่ใช้ Invoice State Diagram เป็นรายการส่งของ MVP เพราะ Finance/Invoice อยู่นอกขอบเขตระบบปัจจุบัน

## การเปิดดูและส่งมอบ

Source diagram เป็น Mermaid ใน Markdown เปิดไฟล์ใน GitHub แบบ Preview เพื่อดูภาพ และเก็บ source ไว้คู่กับภาพที่ export เมื่อทีมจัดชุดส่งมอบ ไฟล์ภาพ export ยังไม่ถูกสร้างในรอบนี้

อ้างอิงภาพรวมที่ [Design Patterns](../design-patterns.md), [Data Dictionary](../data-dictionary.md) และ [Use Case Description](../use-case-description.md)

