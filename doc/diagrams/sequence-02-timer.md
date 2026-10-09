# Sequence 02: Start และ Stop Timer

ที่มา: [Kompat](../V1/USECASE/kompat-usecase.md) ปรับ current-user abstraction ให้ตรง TimerController ณ `cb8002d` ขั้น start/stop อยู่ใน transaction; progress listener รับ event หลัง commit ดังแสดงใน [Sequence 06](sequence-06-progress-events.md)

```mermaid
sequenceDiagram
    actor F as Freelancer
    participant C as TimerController
    participant U as CurrentUserProvider
    participant S as TimerServiceImpl
    participant P as Project/Task Repository
    participant R as TimeEntryRepository
    participant E as TimeEntry
    participant T as Task
    participant B as ApplicationEventPublisher

    F->>C: POST /api/timer/start + Bearer JWT
    C->>U: currentUserId()
    U-->>C: ownerId
    C->>S: startTimer(ownerId, request)
    S->>P: validate owned Project/Task
    S->>E: startTimer(..., Instant.now(clock))
    S->>R: check running timer
    S->>R: saveAndFlush(entry)
    opt ส่ง Task
        S->>T: start() (OPEN เปลี่ยน, IN_PROGRESS คงเดิม, COMPLETED ปฏิเสธ)
        opt สถานะเปลี่ยน
            S->>P: saveAndFlush(task)
        end
    end
    S-->>C: TimeEntryResponse
    C->>C: StartedTimerResponse.from(response)
    C-->>F: 201 Created + ApiResult.data

    F->>C: POST /api/timer/stop + Bearer JWT
    C->>S: stopTimer(ownerId)
    S->>R: find locked running timer
    R-->>S: TimeEntry
    S->>E: stop(Instant.now(clock))
    S->>B: publish TimerStoppedEvent
    S-->>C: TimeEntryResponse
    C-->>F: 200 OK + ApiResult.data
```
