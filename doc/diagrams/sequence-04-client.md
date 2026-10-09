# Sequence 04: Soft-delete Client

ที่มา: [Thirawat](../V1/USECASE/thirawat-usecase.md) ณ `131305f` Diagram เริ่มหลังผ่าน JWT; RequestTraceFilter ทำงานก่อน security/MVC สำเร็จคืน 204 ไม่มี body และไม่เปลี่ยน isActive หรือสถานะ Project

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

    F->>M: DELETE /api/clients/{id} + Bearer JWT
    M->>C: softDelete(clientId)
    C->>U: currentUserId()
    U-->>C: ownerId
    C->>S: softDelete(ownerId, clientId)
    S->>R: findByIdAndOwnerId(clientId, ownerId)
    R-->>S: Client หรือ empty
    alt Client ของ owner และ deletedAt เป็น null
        S->>E: softDelete() ตั้ง deletedAt
        S->>R: save(Client)
        S-->>C: void
        C-->>M: 204 No Content
        M-->>F: 204 No Content
    else ไม่พบหรือเป็นของผู้อื่น
        S-->>C: ClientNotFoundException
        C-->>M: ClientNotFoundException
        M->>H: handle ClientNotFoundException
        H->>A: response(404, message, CLIENT_NOT_FOUND, null)
        A-->>H: ApiResult พร้อม error metadata
        H-->>M: 404 ApiResult
        M-->>F: 404 ApiResult + X-Request-ID
    end
```
