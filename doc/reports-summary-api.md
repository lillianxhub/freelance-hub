# Reports API Contract

เอกสารนี้อธิบาย API ที่หน้า **รายงาน** ใช้ในโค้ดปัจจุบัน

ตรวจเทียบ ReportController, ReportServiceImpl, ReportQueryRepository และ ReportsPage ณ commit `dbcc4b9` วันที่ 10 ตุลาคม 2026; เป็น contract ที่ตรวจจาก source ไม่ใช่หลักฐานผลรันของ deployment

## API ที่หน้า Reports เรียก

หน้า Reports เรียก 3 endpoint เมื่อเปิดหน้า หรือเปลี่ยนตัวกรอง และเรียก `/projects` อีกครั้งเมื่อเปลี่ยนหน้าตาราง:

| Endpoint | ข้อมูลที่ใช้ |
|---|---|
| `GET /api/reports/summary` | KPI และตัวเลือกใน dropdown ลูกค้า/โปรเจกต์ |
| `GET /api/reports/distribution?groupBy=CLIENT` หรือ `PROJECT` | กราฟเวลาตามลูกค้าหรือโปรเจกต์ตามปุ่มที่ผู้ใช้เลือก |
| `GET /api/reports/projects?page=1&limit=10` | กราฟการใช้ชั่วโมงและตารางโปรเจกต์ พร้อม `meta` สำหรับ pagination |

Backend ยังมี `GET /api/reports/work-trend` และ `GET /api/reports/work-pattern` แต่หน้า Reports ปัจจุบันไม่ได้แสดงข้อมูลจากสองเส้นนี้ จึงไม่เรียก

ไม่ต้องเรียก `/api/clients`, `/api/projects`, `/api/time-entries` หรือ task ของแต่ละโปรเจกต์มาเพื่อประกอบรายงานใน Frontend

## ตัวกรอง

ทั้งห้า Reports endpoints รับตัวกรองต่อไปนี้ โดย `/work-trend` บังคับส่งวันที่ทั้งคู่:

| Parameter | รูปแบบ | ความหมาย |
|---|---|---|
| `from` และ `to` | `YYYY-MM-DD` | ส่งทั้งคู่เพื่อดูช่วงวันที่ โดยรวมวันเริ่มและวันสิ้นสุดตามเขตเวลา Asia/Bangkok; เว้นทั้งคู่เพื่อดูทุกช่วงเวลา |
| `clientId` | UUID | จำกัดข้อมูลเฉพาะลูกค้า |
| `projectId` | UUID | จำกัดข้อมูลเฉพาะโปรเจกต์ |
| `status` | `PLANNED`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED` | กรองตามสถานะปัจจุบันของ Project; ไม่ส่งหมายถึงทุกสถานะ |

หากส่งวันที่เพียงข้างเดียว หรือ `from` หลัง `to` Backend ตอบ 400 ส่วน Frontend ไม่ส่ง request ระหว่างที่ตัวกรองวันที่ยังไม่ครบหรือไม่ถูกต้อง ค่า `ALL` เป็นค่าใน UI เท่านั้น ไม่ส่งไป API

`/distribution` รับ `groupBy=CLIENT` หรือ `PROJECT`; `/projects` รับ `page` เริ่มที่ 1, `limit` เริ่มต้น 10, `sortBy` และ `direction` เพิ่มเติม `/work-trend` ต้องระบุวันที่ทั้งคู่ และรับ `granularity=DAY|WEEK|MONTH`

ช่วงวันที่กรองด้วย startedAt และนับ durationSeconds ทั้งรายการ ไม่ใช่หาส่วนของเวลาที่ทับซ้อนช่วงหรือแบ่งรายการข้ามวัน `/projects` รับ sortBy เป็น `projectName`, `clientName`, `trackedSeconds`, `usagePercent`; direction รับ asc/desc โดยไม่สนตัวพิมพ์

## รูปแบบ Response

ทุกเส้นใช้ wrapper กลาง `{ success, message, data, meta, error }` โดย `/projects` คืน array ใน `data` และ pagination ใน `meta`:

```json
{
  "success": true,
  "message": "ดึงรายงานโปรเจกต์สำเร็จ",
  "data": [
    {
      "projectId": "project-uuid",
      "projectName": "Website Redesign",
      "clientId": "client-uuid",
      "clientName": "Northstar Studio",
      "color": "#4F6BFF",
      "targetSeconds": 144000,
      "trackedSeconds": 100800,
      "usagePercent": 70.00,
      "taskProgressPercent": 75.00,
      "status": "ACTIVE"
    }
  ],
  "meta": { "page": 1, "limit": 10, "total": 1, "totalPages": 1 },
  "error": null
}
```

`/summary` คืน `data.filters.projects[]` ที่มี `id`, `name`, `clientId`, `status` และคืน `data.generatedAt`, `data.filters.clients`, `data.filters.projects` และ `data.summary` ซึ่งมี `totalTrackedSeconds`, `trackedTimeTrendPercent`, `timeEntryCount`, `projectsWithTime`, `totalProjects`, `clientsWithTime`, `totalClients` ส่วน `/distribution` คืน `data.groupBy` และ `data.items` ที่มี `id`, `name`, `trackedSeconds`, `percent`

`trackedTimeTrendPercent` เป็น `null` เมื่อดูทุกช่วงเวลา เพราะไม่มีช่วงก่อนหน้าให้เทียบ `targetSeconds` และ `usagePercent` เป็น `null` เมื่อโปรเจกต์ไม่มีเป้าหมาย ชั่วโมงที่ใช้คิดจากวินาทีที่ Backend ส่งมา และความคืบหน้างานคิดจากจำนวน task ที่เสร็จต่อ task ทั้งหมด

เมื่อระบุวันที่ trend เทียบช่วงก่อนหน้าที่ติดกันและยาวเท่ากัน ใช้ `(current-previous)/previous*100`; หากยอดช่วงก่อนเป็น 0 ให้ trend เป็น 0 เมื่อยอดปัจจุบันเป็น 0 หรือ 100 เมื่อยอดปัจจุบันมากกว่า 0 ต่างจาก Dashboard ซึ่งคืน null ในกรณี previous=0 taskProgressPercent นับเฉพาะ Task ที่ active และไม่ถูก soft delete; ไม่มี Task ให้เปอร์เซ็นต์เป็น 0

## จุดที่ต้องระวัง

- ข้อมูลทุก query ถูกจำกัดด้วย owner จาก access token; ยอดเวลาไม่รวม Time Entry ที่ยังไม่จบ/inactive/ถูก soft delete หรือ Project/Client ที่ถูก soft delete แต่ยังรวม Client/Project ที่ archive และเวลาเดิมบน Task ที่ถูก soft delete เพราะ time query ไม่กรอง Task
- Dropdown options จาก summary เป็น Client/Project ทั้งหมดที่มองเห็น ไม่กรองวันที่/status ฝั่ง Backend; Frontend กรอง Project options ด้วย clientId และ status เมื่อเปลี่ยน Client หรือ status จะล้าง Project ที่เลือกและกลับหน้า 1
- จำนวน totalProjects จำกัดด้วย clientId/projectId/status; totalClients เมื่อระบุ status นับ Client ที่มี Project เข้าเงื่อนไข แม้ไม่มีเวลาในช่วงนั้น วันที่จำกัดยอดเวลาและจำนวนรายการที่มีเวลา แต่ไม่ซ่อน Project ที่ไม่มีเวลา
- status ใช้สถานะปัจจุบันของ Project ไม่ใช่สถานะ ณ วันที่บันทึกเวลา และใช้กับทั้งช่วงปัจจุบัน/ช่วงก่อนหน้าของ trend; status ผิด enum ได้ 400 ส่วน Project ที่มีสิทธิ์อ่านแต่ไม่ตรง status ให้ผลว่าง ไม่ใช่ 404
- ยอด Reports ไม่จำเป็นต้องเท่ากับ Dashboard/Client GET: ดู [ตารางขอบเขตยอดเวลา](use-case-description.md#ตารางขอบเขตยอดเวลา) ก่อนเทียบตัวเลข
- Repository อ่าน entity ของ Time Entry และ Task ตามเจ้าของและตัวกรอง แล้ว Service รวมยอดใน Java โดยดึงเป็นชุด ไม่ดึงทีละโปรเจกต์
- ตารางและกราฟเปรียบเทียบโปรเจกต์แสดงเฉพาะหน้า pagination ปัจจุบัน ปุ่ม CSV จึงส่งออกเฉพาะหน้านั้น
- `getProjects` ใน Backend ยังโหลดโปรเจกต์ที่มองเห็นทั้งหมดเพื่อเรียงและตัดหน้าในหน่วยความจำ หากจำนวนโปรเจกต์มากควรย้าย pagination ไปที่ฐานข้อมูล
- `ReportQueryRepository` เป็น Spring Data interface ใช้ @Query อ่านข้อมูลสำหรับ Report โดยเฉพาะ; Service กรอง client/project/status และจัดกลุ่มวัน/ชั่วโมงใน Asia/Bangkok

## ตำแหน่งโค้ด

- Frontend: `code/Frontend/src/Analytics/pages/Reports/page.tsx`, `code/Frontend/src/services/report.ts`, `code/Frontend/src/types/analytics.ts`
- Backend: `code/Backend/src/main/java/th/ac/kku/freelance_hub/controller/ReportController.java`, `service/impl/ReportServiceImpl.java`, `repository/ReportQueryRepository.java`


## Backend report query contract

- `ReportServiceImpl` uses one report-only Spring Data `ReportQueryRepository` interface with @Query read methods. Shared Client/Project/Task/TimeEntry repositories are unchanged.
- Repository extends Repository<TimeEntry, UUID> and reads entities using explicit JPQL @Query methods; project reads fetch the client and task/entry reads fetch the project. Service selects project IDs and handles calculations, grouping, sorting and pagination. Owner, soft deletion, completed-entry and date constraints remain in database reads.
- `ReportServiceImpl` aggregates totals, groups days/hours/projects, counts completed tasks, sorts the mapped responses, and slices the requested page. `ReportMapper` converts service-calculated project metrics to API responses. Usage sorting uses the displayed two-decimal value; missing/zero targets sort last in both directions. Ties use project ID, and pages beyond the end retain the total count.
- Distribution filters client/project/status in the service using visible owned projects. Each endpoint loads project metadata once (apart from a targeted ownership check for a supplied project ID), and reuses the selection for current/previous entries. Summary returns complete dropdown options.
- Work trend requires both dates and permits at most 366 buckets for DAY/WEEK/MONTH. A larger range returns HTTP 400; choose a coarser granularity or shorter range.
- Work trend/work pattern attribute each completed entry's entire stored duration to its start date/hour in Asia/Bangkok. They do not split sessions across midnight or hourly boundaries. Date filters also select by start time. This keeps time totals consistent with the entry-based summary.
- Previous-period comparison is null when the previous total is zero, including when both totals are zero; an undefined percentage is not reported as 100%.
- Controller integration tests exercise the actual security filter chain and shared error handler. Database integration tests use H2; PostgreSQL execution plans still need validation against the deployment database.
- This design processes every matching source row in Java. All-time reports therefore use memory proportional to the user's matching entries/tasks; database aggregation may be preferable if data volume grows significantly. It does not add frontend API requests.
