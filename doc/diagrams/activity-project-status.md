# Activity: เปลี่ยนสถานะ Project

ที่มา: [Kantavit](../V1/DESIGN/kantavit-design.md) ณ `131305f` ใช้กับ Project API; Client archive cascade เรียก Project.changeStatus โดยตรงจึงไม่ผ่าน running-timer guard นี้

```mermaid
flowchart TD
    A[ผู้ใช้ส่งคำขอเปลี่ยนสถานะ Project] --> B[ProjectServiceImpl โหลด Project ของ owner ที่ยังไม่ถูก soft delete]
    B --> C{Project นี้มี timer กำลังทำงาน?}
    C -- มี --> D[คืน 409 และให้หยุด timer ก่อน]
    C -- ไม่มี --> E{ACTIVE → COMPLETED?}
    E -- ใช่ --> N[TaskRepository นับ Task ที่ยังใช้งานและ Task ที่เสร็จ]
    N --> O{ไม่มี Task ที่ยังไม่เสร็จ?}
    O -- ไม่ --> G[คืน 409]
    O -- ใช่ --> P[ProjectStates เลือก State ปัจจุบัน]
    E -- ไม่ --> P
    P --> F{State อนุญาตสถานะใหม่?}
    F -- ไม่ --> G[คืน 409]
    F -- ใช่ --> H{กำลังคืนจาก ARCHIVED?}
    H -- ใช่ --> I{Client ยัง active และไม่ถูก soft delete?}
    I -- ไม่ --> G
    I -- ใช่ --> J[เปลี่ยน status และ isActive]
    H -- ไม่ --> J
    J --> K{เพิ่งเปลี่ยนเป็น COMPLETED?}
    K -- ใช่ --> L[TimeEntryService.lockByProject ใน transaction เดียวกัน]
    K -- ไม่ --> M[บันทึก Project และคืน response]
    L --> M
```
