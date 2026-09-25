# Freelance Hub MVP — Domain Class Diagram

Class Diagram นี้เน้น JPA Entity และพฤติกรรมของ domain ไม่รวม Controller, DTO,
Service และ Repository เพื่อให้เห็นโครงสร้างข้อมูลหลักชัดเจน

```mermaid
classDiagram
    direction LR

    class User {
        +UUID id
        +String email
        -String passwordHash
        +UserRole role
        +UserStatus status
        +boolean enabled
        +changePassword(passwordHash)
        +disable()
    }
    class UserProfile {
        +UUID userId
        +String displayName
        +String phone
        +String address
        +String firstName
        +String lastName
        +String profileImageUrl
        +String bio
        +String timezone
        +String dateFormat
        +updateContact(...)
        +changePreferences(...)
    }
    class Client {
        +UUID id
        +UUID ownerId
        +String name
        +String companyName
        +String email
        +String phone
        +String address
        +String taxId
        +String notes
        +ClientStatus status
        +updateDetails(...)
        +archive()
    }
    class Project {
        +UUID id
        +UUID ownerId
        +UUID clientId
        +String name
        +String description
        +LocalDate startDate
        +LocalDate endDate
        +String color
        +Currency currency
        +Integer targetMinutes
        +ProjectStatus status
        +changeStatus(nextStatus)
        +canTrackTime() boolean
        +progress(trackedMinutes) decimal
        +archive()
    }
    class Task {
        +UUID id
        +UUID projectId
        +String name
        +String description
        +TaskStatus status
        +int sortOrder
        +Instant completedAt
        +start()
        +complete(at)
        +reorder(position)
    }
    class TimeEntry {
        +UUID id
        +UUID ownerId
        +UUID projectId
        +UUID taskId
        +String description
        +EntryType entryType
        +Instant startedAt
        +Instant endedAt
        +Integer durationMinutes
        +Instant lockedAt
        +stop(at)
        +changeDetails(...)
        +lock(at)
        +isRunning() boolean
    }
    class UserRole {
        <<enumeration>>
        USER
        ADMIN
    }
    class UserStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
        SUSPENDED
    }
    class ClientStatus {
        <<enumeration>>
        ACTIVE
        ARCHIVED
    }
    class ProjectStatus {
        <<enumeration>>
        PLANNED
        ACTIVE
        ON_HOLD
        COMPLETED
        ARCHIVED
    }
    class TaskStatus {
        <<enumeration>>
        OPEN
        IN_PROGRESS
        COMPLETED
    }
    class EntryType {
        <<enumeration>>
        TIMER
        MANUAL
    }

    User "1" *-- "1" UserProfile : profile
    User "1" --> "0..*" Client : owns
    User "1" --> "0..*" Project : owns
    User "1" --> "0..*" TimeEntry : owns
    Client "1" --> "0..*" Project : projects
    Project "1" *-- "0..*" Task : tasks
    Project "1" --> "0..*" TimeEntry : entries
    Task "0..1" --> "0..*" TimeEntry : entries
    User --> UserRole
    User --> UserStatus
    Client --> ClientStatus
    Project --> ProjectStatus
    Task --> TaskStatus
    TimeEntry --> EntryType
```

## แนวทาง JPA

- Association ทุกตัวใช้ `FetchType.LAZY`; โหลด graph หรือ projection เฉพาะหน้าที่ต้องใช้
- ไม่ใช้ `CascadeType.ALL` กับข้อมูลประวัติ: อนุญาต `PERSIST/MERGE` ระหว่าง `User` กับ `UserProfile` ได้ แต่ไม่ cascade remove จาก Client/Project ไปยังประวัติ
- ใส่ `@Version` ใน mutable entities เพื่อป้องกัน lost update โดยเฉพาะ `Project` และ `TimeEntry`
- Service ต้องตรวจ ownership ทุกครั้ง แม้ฐานข้อมูลจะมี composite FK ช่วยรักษาความสอดคล้องอยู่แล้ว
- Domain method เป็นผู้ควบคุม transition และ invariant; setter ของ status/time ไม่ควรเปิดเป็น public

