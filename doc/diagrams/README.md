# Diagram Index - Freelance Hub

รวม diagram ของฉบับส่งมอบและตรวจชื่อ class/method/response กับ implementation ณ commit `5f55faf` วันที่ 9 ตุลาคม 2026 รวมการปรับเอกสาร Petpinyo ใน PR #127 ข้อความภายใน Mermaid diagrams ใช้ภาษาอังกฤษ ส่วนคำอธิบายภายนอกคงภาษาไทย

## Diagram ที่จัดไว้แล้ว

| ประเภท | ไฟล์ | ที่มา / ขอบเขต |
|---|---|---|
| Domain Class Model | [Class Diagram](class-diagram.md) | Domain model เดิม ตรวจ Entity methods และ embedded Address แล้ว |
| Class Diagram + Patterns | [Application/Authentication/State](class-diagram.md#application-layers) | รวมจาก diagrams ของสมาชิกและแสดง pattern placement |
| ER / Database Schema | [ER Diagram](er-diagram.md) | Schema หลัง migrations V1-V20; Data Dictionary เป็นรายละเอียด constraints |
| Sequence 01 | [Login และ protected request](sequence-diagram.md#scenario-01-login-and-protected-request) | Petpinyo |
| Sequence 02 | [Start และ Stop Timer](sequence-diagram.md#scenario-02-start-and-stop-timer) | Kompat |
| Sequence 03 | [เปลี่ยนสถานะ Project](sequence-diagram.md#scenario-03-change-project-status) | Kantavit |
| Sequence 04 | [Soft-delete Client](sequence-diagram.md#scenario-04-soft-delete-client) | Thirawat |
| Sequence 05 | [Reports และตัวกรอง](sequence-diagram.md#scenario-05-load-reports-and-change-filters) | Nattadol |
| Sequence 06 | [Progress Events](sequence-diagram.md#scenario-06-progress-threshold-events) | Kantavit / Time Tracking events |
| Activity | [Timer](activity-timer.md), [Project status](activity-project-status.md) | Kompat และ Kantavit |
| Component view | [Dashboard/Reports](component.md) | Nattadol; ขอบเขต Frontend/API/Database ไม่ใช่ full Deployment Diagram |
| Use Case Diagram | [Use Cases แยกตาม feature](use-case-diagram.md) | Actors/system boundary และ IDs ตรงกับฉบับรวม; ไม่สร้าง use case ของ feature ที่ยังไม่ implement |
| Conceptual Domain Model | [Domain Model](domain-model.md) | คำศัพท์ธุรกิจ/ความสัมพันธ์ ไม่ใช่ schema หรือ class members ทุกตัว |
| Deployment Diagram | [Cloud และ Local Nodes](deployment-diagram.md) | Vercel, Render, Supabase, Compose และ deployment channels ตาม configuration |
| State Diagram | [Project/Client/Task/Time Entry](state-diagram.md) | Transitions, guards, activation, soft delete และ locking ตาม implementation |

Sequence ทั้ง 6 scenarios อยู่ใน [sequence-diagram.md](sequence-diagram.md) ไฟล์เดียว แยกด้วยหัวข้อและ Mermaid block ของแต่ละ scenario ครบจำนวนขั้นต่ำ 3 scenarios ในใบงาน ตรวจ syntax ด้วย Mermaid 11.12.0 ทั้ง 29 blocks ใน doc/ และ doc/diagrams/ การตรวจนี้ไม่แทนการตรวจ layout ของภาพที่ renderer ของการส่งงานแสดงจริง

## ขอบเขตของ diagram ที่เพิ่ม

Use Case/Domain/Deployment/State source diagrams ที่เคยขาดเพิ่มจาก source ณ ca77d74 แล้ว ไม่ใช่การคัดลอก diagram ของระบบอื่นมาใช้ ไม่มี Admin/Invoice/Workspace/Notification endpoints ที่ยังไม่ implement และ Deployment nodes เป็น logical view ที่ต้องเทียบ cloud settings/health จริงก่อนส่ง

ไม่ใช้ Invoice State Diagram เป็นรายการส่งของ MVP เพราะ Finance/Invoice อยู่นอกขอบเขตระบบปัจจุบัน

## การเปิดดูและส่งมอบ

Source diagram เป็น Mermaid ใน Markdown เปิดไฟล์ใน GitHub แบบ Preview เพื่อดูภาพ และเก็บ source ไว้คู่กับภาพที่ export เมื่อทีมจัดชุดส่งมอบ ไฟล์ภาพ export ยังไม่ถูกสร้างในรอบนี้

อ้างอิงภาพรวมที่ [Design Patterns](../design-patterns.md), [Data Dictionary](../data-dictionary.md) และ [Use Case Description](../use-case-description.md)

