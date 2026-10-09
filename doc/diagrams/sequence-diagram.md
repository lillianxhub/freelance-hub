# Sequence Diagrams - Freelance Hub

รวม 6 scenarios ของระบบไว้ไฟล์เดียว ตรวจ controllers/services/events กับ source ณ `5f55faf` วันที่ 9 ตุลาคม 2026 และนำเอกสาร Auth/Profile ที่เพื่อนปรับใน PR #127 มาเทียบกับโค้ดจริง ข้อความในภาพใช้ภาษาอังกฤษ ส่วนคำอธิบายรอบภาพคงภาษาไทย

ทุกภาพเป็น flow ที่ย่อขั้นตอนเพื่ออ่านง่าย ไม่ใช่ทุก method/error path ของระบบ รายละเอียด validation, ownership, HTTP contracts และข้อจำกัดดู [Use Case Description](../use-case-description.md)

## Scenario 01: Login and Protected Request

ที่มา: Petpinyo (`doc/V1/USECASE/petpinyo-usecase.md`; ต้นฉบับ local/ประวัติ Git) เพิ่มขั้น issue refresh token และสร้าง Set-Cookie ให้ตรงกับโค้ด Login ใช้ transaction; JWT filter ของ protected request ตรวจ token/JTI และโหลด user ที่ enabled ภาพแสดง happy path ไม่รวม rate-limit/invalid-credentials branches

```mermaid
sequenceDiagram
    actor User
    participant C as AuthController
    participant S as AuthServiceImpl
    participant A as AuthenticationManager
    participant P as DaoAuthenticationProvider
    participant D as CustomUserDetailsService
    participant R as UserRepository
    participant T as RefreshTokenService
    participant J as JwtTokenProvider
    participant Cookie as RefreshTokenCookie
    participant F as JwtAuthenticationFilter
    participant U as UserController

    User->>C: POST /api/auth/login
    C->>S: login(request, remoteAddress)
    S->>S: Normalize email and check login limiter
    S->>A: authenticate(email, password)
    A->>P: Authenticate credentials
    P->>D: loadUserByUsername(email)
    D-->>P: UserDetails and authorities
    P-->>A: Authenticated principal
    A-->>S: Authentication
    S->>R: findByEmail(email)
    R-->>S: User
    S->>T: issue(user)
    T-->>S: Raw refresh token and expiry
    S->>J: generateToken(email)
    J-->>S: Access JWT
    S->>S: Map user response and record login success
    S-->>C: AuthSessionResult (after transaction commit)
    C->>Cookie: set(refreshToken, expiresAt)
    Cookie-->>C: Set-Cookie header value (HttpOnly)
    C-->>User: 200 ApiResult AuthResponse and Set-Cookie

    User->>F: GET /api/users/me with Bearer JWT
    F->>J: validateToken / getJtiFromToken / getEmailFromToken
    J-->>F: Valid token with JTI and email
    F->>D: loadUserByUsername(email)
    D-->>F: Enabled UserDetails
    F->>F: Set authenticated SecurityContext
    F->>U: Continue filter chain and MVC dispatch
    U-->>User: 200 ApiResult UserResponse
```

Refresh token เก็บเฉพาะ hash ในฐานข้อมูล ไม่ส่ง raw token ใน JSON; User/Profile โหลดและบันทึกผ่าน UserRepository ไม่ใช่เรียก UserProfileRepository ใน flow นี้ อายุ JWT/refresh ปรับผ่าน configuration ได้

## Scenario 02: Start and Stop Timer

ที่มา: Kompat (`doc/V1/USECASE/kompat-usecase.md`; ต้นฉบับ local/ประวัติ Git) current-user abstraction ตรง TimerController ขั้น start/stop อยู่ใน transaction การ start Task ทำหลัง insert timer สำเร็จและ rollback ร่วมกันหาก Task ปฏิเสธ ดูการรับ event หลัง commit ใน [Scenario 06](#scenario-06-progress-threshold-events)

```mermaid
sequenceDiagram
    actor F as Freelancer
    participant C as TimerController
    participant U as CurrentUserProvider
    participant S as TimerServiceImpl
    participant P as User / Project / Task Repositories
    participant R as TimeEntryRepository
    participant E as TimeEntry
    participant T as Task
    participant B as ApplicationEventPublisher

    F->>C: POST /api/timer/start with Bearer JWT
    C->>U: currentUserId()
    U-->>C: ownerId
    C->>S: startTimer(ownerId, request)
    S->>P: Load owner, owned Project and optional Task
    P-->>S: Valid related entities
    S->>E: startTimer(..., Instant.now(clock))
    Note over S,E: Entity checks Project tracking permission and Client.isActive
    S->>R: Check existing running timer
    R-->>S: No running timer
    S->>R: saveAndFlush(entry)
    R-->>S: Saved TimeEntry
    opt Task supplied
        S->>T: start()
        Note over S,T: OPEN becomes IN_PROGRESS, IN_PROGRESS is unchanged, COMPLETED is rejected
        opt Task status changed
            S->>P: saveAndFlush(task)
        end
    end
    S-->>C: TimeEntryResponse (after transaction commit)
    C->>C: StartedTimerResponse.from(response)
    C-->>F: 201 ApiResult StartedTimerResponse

    F->>C: POST /api/timer/stop with Bearer JWT
    C->>U: currentUserId()
    U-->>C: ownerId
    C->>S: stopTimer(ownerId)
    S->>R: Find running timer with pessimistic write lock
    R-->>S: TimeEntry
    S->>E: stop(Instant.now(clock))
    E-->>S: endedAt and positive durationSeconds
    S->>B: publishEvent(TimerStoppedEvent)
    Note over S,B: Event is published inside the transaction, progress listener runs AFTER_COMMIT
    S-->>C: TimeEntryResponse (after transaction commit)
    C-->>F: 200 ApiResult TimeEntryResponse
```

Partial unique index ของ PostgreSQL กัน timer ซ้อนของ owner เดียวแม้สองคำขอแข่งกัน; service แปลง constraint violation นี้เป็น conflict ไม่ใช่ป้องกันด้วย pre-check อย่างเดียว Cancel timer ลบ running row จริงและไม่ publish TimerStoppedEvent

## Scenario 03: Change Project Status

ที่มา: Kantavit (`doc/V1/USECASE/kantavit-usecase.md`; ต้นฉบับ local/ประวัติ Git) การค้นหา owner, ตรวจ running timer, ตรวจ Task และ lockByProject อยู่ใน Service transaction เดียวกัน ภาพแสดง exception ย้อนกลับผ่าน Controller/MVC ก่อน dispatch ไป GlobalExceptionHandler ไม่ใช่ Service เรียก handler เอง

```mermaid
sequenceDiagram
    actor F as Freelancer
    participant M as Spring MVC
    participant C as ProjectController
    participant U as CurrentUserProvider
    participant S as ProjectServiceImpl
    participant R as ProjectRepository
    participant TR as TaskRepository
    participant T as TimerService
    participant P as Project
    participant ST as ProjectStates / ProjectState
    participant TE as TimeEntryService
    participant H as GlobalExceptionHandler

    F->>M: PATCH /api/projects/{id}/status with JWT
    M->>C: changeStatus(id, request)
    C->>U: currentUserId()
    U-->>C: ownerId
    C->>S: changeStatus(ownerId, id, request)
    S->>R: findByIdAndOwnerId(id, ownerId)
    R-->>S: Owned, non-deleted Project
    S->>T: getCurrentTimer(ownerId)
    T-->>S: Optional running timer
    break Running timer belongs to this Project
        S-->>C: IllegalStateException (transaction rollback)
        C-->>M: Propagate exception
        M->>H: Resolve IllegalStateException
        H-->>M: 409 ApiResult
        M-->>F: 409 Conflict
    end
    opt ACTIVE to COMPLETED
        S->>TR: summarizeProgressByProjectIds(ownerId, [id], COMPLETED)
        TR-->>S: Active Task count and completed count
        break An active Task is incomplete
            S-->>C: IllegalStateException (transaction rollback)
            C-->>M: Propagate exception
            M->>H: Resolve IllegalStateException
            H-->>M: 409 ApiResult
            M-->>F: 409 Conflict, status unchanged
        end
    end
    S->>P: changeStatus(nextStatus)
    P->>ST: from(status), canTransitionTo(nextStatus)
    ST-->>P: Allowed or rejected
    opt Restoring from ARCHIVED with a valid transition
        P->>P: Check Client.isActive and Client.deletedAt
    end
    alt Transition and applicable Client guard pass
        P-->>S: Update status and isActive
        opt Newly entered COMPLETED
            S->>TE: lockByProject(ownerId, id)
            TE-->>S: Existing entries locked in the same transaction
        end
        S->>R: save(project)
        R-->>S: Saved Project
        S-->>C: ProjectResponse (after transaction commit)
        C-->>M: 200 ApiResult
        M-->>F: 200 ApiResult
    else Invalid transition or failed restore guard
        P-->>S: IllegalStateException
        S-->>C: Exception (transaction rollback)
        C-->>M: Propagate exception
        M->>H: Resolve IllegalStateException
        H-->>M: 409 ApiResult
        M-->>F: 409 Conflict
    end
```

หาก lockByProject พบ running entry ก็ปฏิเสธและ rollback ทั้ง transaction ส่งสถานะเดิมไม่ล็อกซ้ำแต่ยังตรวจ running timer; archive cascade จาก Client ใช้ Project.changeStatus โดยตรง ไม่ผ่าน ProjectService guard นี้

## Scenario 04: Soft-delete Client

ที่มา: Thirawat (`doc/V1/USECASE/thirawat-usecase.md`; ต้นฉบับ local/ประวัติ Git) เริ่มหลังผ่าน JWT; RequestTraceFilter ทำงานก่อน security/MVC สำเร็จคืน 204 ไม่มี body โดยไม่เปลี่ยน Client.isActive หรือสถานะ Project

```mermaid
sequenceDiagram
    actor F as Freelancer
    participant M as Spring MVC
    participant C as ClientController
    participant U as CurrentUserProvider
    participant S as ClientServiceImpl
    participant R as ClientRepository
    participant E as Client
    participant H as ClientExceptionHandler
    participant A as ApiErrorFactory

    F->>M: DELETE /api/clients/{id} with Bearer JWT
    M->>C: softDelete(clientId)
    C->>U: currentUserId()
    U-->>C: ownerId
    C->>S: softDelete(ownerId, clientId)
    S->>R: findByIdAndOwnerId(clientId, ownerId)
    R-->>S: Client or empty
    alt Owned Client exists and deletedAt is null
        S->>E: softDelete() sets deletedAt only
        S->>R: save(client)
        S-->>C: void (after transaction commit)
        C-->>M: 204 No Content
        M-->>F: 204 No Content
    else Missing, deleted or owned by another user
        S-->>C: ClientNotFoundException
        C-->>M: Propagate exception
        M->>H: Resolve ClientNotFoundException
        H->>A: response(404, message, CLIENT_NOT_FOUND, null)
        A-->>H: ApiResult with error metadata
        H-->>M: 404 ApiResult
        M-->>F: 404 ApiResult and X-Request-ID
    end
```

## Scenario 05: Load Reports and Change Filters

ที่มา: Nattadol (`doc/V1/USECASE/nattadol-usecase.md`; ต้นฉบับ local/ประวัติ Git) การเรียกสาม endpoint เป็นอิสระ ไม่ได้บังคับให้รอทีละ request ภาพใช้ par เพื่อสื่อการโหลดพร้อมกัน; pagination/CSV ใช้ Project ของหน้าปัจจุบัน

```mermaid
sequenceDiagram
    actor User
    participant Page as ReportsPage
    participant Service as services/report.ts
    participant API as ReportController
    participant Logic as ReportServiceImpl

    User->>Page: Open page or change filters
    par Summary
        Page->>Service: getReportSummary(query)
        Service->>API: GET /api/reports/summary with query
        API->>Logic: getSummary(ownerId, filter)
        Logic-->>API: ReportSummaryResponse
        API-->>Service: ApiResult data
        Service-->>Page: Typed summary
    and Time distribution
        Page->>Service: getReportDistribution(query, groupBy)
        Service->>API: GET /api/reports/distribution with query
        API->>Logic: getDistribution(ownerId, filter, groupBy)
        Logic-->>API: ReportDistributionResponse
        API-->>Service: ApiResult data
        Service-->>Page: Typed distribution
    and Project page
        Page->>Service: getReportProjects(query, page)
        Service->>API: GET /api/reports/projects with query
        API->>Logic: getProjects(ownerId, filter, request)
        Logic-->>API: Paginated project results
        API-->>Service: ApiResult data and meta
        Service-->>Page: Typed project page
    end
    Page-->>User: KPIs, distribution chart and project table
```

## Scenario 06: Progress Threshold Events

ที่มา: Kantavit (`doc/V1/DESIGN/kantavit-design.md`; ต้นฉบับ local/ประวัติ Git) Observer ผ่าน Spring events; listener เปิด read-only transaction ใหม่หลัง commit หากไม่มี Project/targetMinutes จะไม่มี threshold event ปลายทางเขียน log ไม่ได้ส่ง notification ให้ผู้ใช้

```mermaid
sequenceDiagram
    participant T as TimerServiceImpl
    participant B as ApplicationEventPublisher / Spring Events
    participant L as TimerStoppedProgressListener
    participant R as ProjectRepository
    participant Q as TimeEntryService
    participant H as ProjectProgressThresholds
    participant O as ProjectProgressThresholdListener

    T->>B: publishEvent(TimerStoppedEvent) inside stop transaction
    Note over T,B: Stop transaction commits before listener delivery
    B->>L: onTimerStopped(event), AFTER_COMMIT
    Note over L,Q: Listener starts REQUIRES_NEW read-only transaction
    L->>R: findByIdAndOwnerId(projectId, ownerId)
    R-->>L: Optional Project
    opt Project exists and targetMinutes is set
        L->>Q: summarize(ownerId, filter.projectId)
        Q-->>L: Current completed totalSeconds
        L->>L: Previous seconds = max(0, current - event duration)
        L->>H: newlyReached(previousMinutes, target, currentMinutes, target)
        H-->>L: Newly crossed 80 and/or 100 thresholds, or empty
        loop Each newly crossed threshold
            L->>B: publishEvent(ProjectProgressThresholdEvent)
            B->>O: onThresholdReached(event), EventListener
            O->>O: Write threshold log
        end
    end
```

การเทียบเกณฑ์แปลง seconds เป็นจำนวน minutes แบบหารจำนวนเต็ม ไม่ใช่ publish ทุกรอบที่เกิน 80%/100% และ Manual Entry ไม่สร้าง TimerStoppedEvent อัตโนมัติ
