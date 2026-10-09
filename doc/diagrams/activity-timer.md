# Activity: เริ่มและหยุด Timer

ที่มา: [Kompat](../V1/DESIGN/kompat-design.md) ณ `131305f` นอกจากการตรวจ running timer ใน service ยังมี partial unique index ของ PostgreSQL ป้องกันคำขอ start พร้อมกัน กรณี Task COMPLETED การบันทึกใน transaction จะ rollback

```mermaid
flowchart TD
    A[User requests timer start] --> B[Controller reads authenticated owner]
    B --> C[Service loads owned Project and optional Task]
    C --> G[Call TimeEntry.startTimer with server Clock]
    G --> D{Project can track time and Client is active?}
    D -- No --> E[Return 409]
    D -- Yes --> F{A running timer already exists?}
    F -- Yes --> E
    F -- No --> H[Insert timer; unique index prevents concurrent timers]
    H --> T{Task supplied?}
    T -- No --> I[Commit and return 201 StartedTimerResponse]
    T -- Yes --> U[Call Task.start]
    U -- COMPLETED --> E2[Return 409 and roll back transaction]
    U -- OPEN --> V[Save Task as IN_PROGRESS]
    U -- IN_PROGRESS --> I
    V --> I
    I --> J[User requests timer stop]
    J --> K[Load running timer with pessimistic write lock]
    K --> L{Timer found?}
    L -- No --> M[Return 404]
    L -- Yes --> N[Set endedAt and calculate durationSeconds]
    N --> O[Publish TimerStoppedEvent]
    O --> P[Commit and return 200 stopped entry]
```
