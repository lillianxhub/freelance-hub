# Activity: เปลี่ยนสถานะ Project

ที่มา: Kantavit ณ `131305f` ใช้กับ Project API; Client archive cascade เรียก Project.changeStatus โดยตรงจึงไม่ผ่าน running-timer guard นี้

```mermaid
flowchart TD
    A[User requests Project status change] --> B[Load owned non-deleted Project]
    B --> C{This Project has a running timer?}
    C -- Yes --> D[Return 409; stop timer first]
    C -- No --> E{ACTIVE to COMPLETED?}
    E -- Yes --> N[Count active Tasks and completed Tasks]
    N --> O{All active Tasks completed?}
    O -- No --> G[Return 409]
    O -- Yes --> P[ProjectStates selects current State]
    E -- No --> P
    P --> F{State permits the next status?}
    F -- No --> G
    F -- Yes --> H{Restoring from ARCHIVED?}
    H -- Yes --> I{Client active and not soft-deleted?}
    I -- No --> G
    I -- Yes --> J[Update status and isActive]
    H -- No --> J
    J --> K{Newly entered COMPLETED?}
    K -- Yes --> L[TimeEntryService.lockByProject in the same transaction]
    K -- No --> M[Save Project; commit and return response]
    L --> M
```
