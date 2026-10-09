# Sequence 03: เปลี่ยนสถานะ Project

ที่มา: [Kantavit](../V1/USECASE/kantavit-usecase.md) ณ `131305f` การค้นหา owner, ตรวจ running timer, ตรวจ Task และ lockByProject อยู่ใน Service transaction เดียวกัน Spring MVC เป็นผู้ส่ง exception ไป handler ไม่ใช่ service เรียก handler โดยตรง

```mermaid
sequenceDiagram
    actor F as Freelancer
    participant C as ProjectController
    participant S as ProjectServiceImpl
    participant R as ProjectRepository
    participant TR as TaskRepository
    participant T as TimerService
    participant P as Project
    participant ST as ProjectStates / ProjectState
    participant TE as TimeEntryService
    participant H as GlobalExceptionHandler
    F->>C: PATCH /api/projects/{id}/status + JWT
    C->>S: changeStatus(ownerId, id, request)
    S->>R: findByIdAndOwnerId(id, ownerId)
    R-->>S: Project ที่ยังไม่ถูก soft delete
    S->>T: getCurrentTimer(ownerId)
    T-->>S: running timer ของ owner หรือว่าง
    alt timer ของ Project กำลังทำงาน
        S-->>H: IllegalStateException
        H-->>F: 409 Conflict
    else ไม่มี timer กำลังทำงาน
        opt ACTIVE → COMPLETED
            S->>TR: summarizeProgressByProjectIds(ownerId, [id], COMPLETED)
            TR-->>S: จำนวน Task ที่ยังใช้งานและจำนวนที่เสร็จ
            break ยังมี Task ที่ไม่เสร็จ
                S-->>H: IllegalStateException
                H-->>F: 409 Conflict (ไม่เปลี่ยนสถานะ)
            end
        end
        S->>P: changeStatus(nextStatus)
        P->>ST: from(status), canTransitionTo(nextStatus)
        ST-->>P: อนุญาตหรือปฏิเสธ
        opt คืนจาก ARCHIVED
            P->>P: ตรวจ Client.isActive และ Client.deletedAt
        end
        alt transition ผ่านและ Client ใช้งาน
            P-->>S: อัปเดต status และ isActive
            opt เพิ่งเปลี่ยนเข้าสู่ COMPLETED
                S->>TE: lockByProject(ownerId, id)
                TE-->>S: ล็อกรายการเวลาใน transaction เดียวกัน
            end
            S->>R: save(Project)
            S-->>C: ProjectResponse
            C-->>F: 200 ApiResult
        else transition ผิดกฎหรือ Client ถูกจัดเก็บ
            P-->>S: IllegalStateException
            S-->>H: Spring ส่ง exception ให้ handler
            H-->>F: 409 Conflict
        end
    end
```
