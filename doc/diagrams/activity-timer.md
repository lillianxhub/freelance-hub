# Activity: เริ่มและหยุด Timer

ที่มา: [Kompat](../V1/DESIGN/kompat-design.md) ณ `cb8002d` นอกจากการตรวจ running timer ใน service ยังมี partial unique index ของ PostgreSQL ป้องกันคำขอ start พร้อมกัน กรณี Task COMPLETED การบันทึกใน transaction จะ rollback

```mermaid
flowchart TD
    A[ผู้ใช้ส่งคำขอเริ่ม timer] --> B[Controller ดึง owner จากผู้ใช้ที่ล็อกอิน]
    B --> C[Service ตรวจ Project, optional Task และสิทธิ์เจ้าของ]
    C --> D{Project จับเวลาได้และ Client active?}
    D -- ไม่ได้ --> E[คืน 409]
    D -- ได้ --> F{มี running timer อยู่แล้ว?}
    F -- มี --> E
    F -- ไม่มี --> G[สร้าง TimeEntry ชนิด TIMER ด้วย server Clock]
    G --> H[บันทึกและให้ unique index กัน timer ซ้อน]
    H --> T{มี Task?}
    T -- ไม่มี --> I[คืน 201 พร้อม StartedTimerResponse]
    T -- มี --> U[เรียก Task.start]
    U -- COMPLETED --> E2[คืน 409 และ rollback]
    U -- OPEN --> V[บันทึก Task ที่เปลี่ยนเป็น IN_PROGRESS]
    U -- IN_PROGRESS --> I
    V --> I
    I --> J[ผู้ใช้ส่งคำขอหยุด timer]
    J --> K[อ่าน running timer พร้อม pessimistic write lock]
    K --> L{พบ timer?}
    L -- ไม่พบ --> M[คืน 404]
    L -- พบ --> N[ตั้ง endedAt และคำนวณ durationSeconds]
    N --> O[เผยแพร่ TimerStoppedEvent]
    O --> P[commit แล้วคืน 200 พร้อมข้อมูลที่หยุด]
```
