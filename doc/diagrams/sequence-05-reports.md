# Sequence 05: โหลด Reports และเปลี่ยน Filter

ที่มา: [Nattadol](../V1/USECASE/nattadol-usecase.md) ณ `131305f` การเรียกสาม endpoint เป็นอิสระ ไม่ได้บังคับลำดับรอทีละ request; pagination/CSV ใช้ Project ของหน้าปัจจุบัน

```mermaid
sequenceDiagram
    actor User as ผู้ใช้
    participant Page as ReportsPage
    participant Service as services/report.ts
    participant API as ReportController
    participant Logic as ReportServiceImpl

    User->>Page: เปิดหน้า/เปลี่ยน filter
    Page->>Service: getReportSummary(query)
    Page->>Service: getReportDistribution(query, groupBy)
    Page->>Service: getReportProjects(query, page)
    Service->>API: GET /api/reports/* + query
    API->>Logic: ownerId + filter
    Logic-->>API: summary / distribution / project page
    API-->>Service: ApiResult data + meta
    Service-->>Page: typed data
    Page-->>User: KPI, กราฟ, ตาราง
```
