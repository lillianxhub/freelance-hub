# Sequence 06: ตรวจเกณฑ์เวลาเมื่อหยุด Timer

ที่มา: [Kantavit](../V1/DESIGN/kantavit-design.md) ณ `131305f` Observer ผ่าน Spring events; ไม่มี targetMinutes จะไม่มี threshold event และปลายทางปัจจุบันบันทึก log ไม่ได้ส่ง notification ให้ผู้ใช้

```mermaid
sequenceDiagram
    participant T as Timer Service (ส่วนของทีม)
    participant E as TimerStoppedEvent
    participant L as TimerStoppedProgressListener
    participant Q as TimeEntryService
    participant H as ProjectProgressThresholds
    participant P as ProjectProgressThresholdEvent
    participant O as ProjectProgressThresholdListener
    T->>E: publish หลังหยุด timer
    E-->>L: รับหลัง transaction commit
    L->>Q: summarize(ownerId, filter.projectId)
    Q-->>L: เวลาที่บันทึกจบแล้ว
    L->>H: newlyReached(ก่อน, หลัง, targetMinutes)
    H-->>L: 80, 100 หรือไม่มีเกณฑ์ใหม่
    opt มีเกณฑ์ใหม่
        L->>P: publish แยกตามเกณฑ์
        P-->>O: @EventListener
        O->>O: เขียน log
    end
```
