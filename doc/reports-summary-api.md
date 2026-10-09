# Reports API Contract

เอกสารนี้อธิบาย API ที่หน้า **รายงาน** ใช้ในโค้ดปัจจุบัน สำหรับผู้พัฒนา Frontend, Backend และ AI ที่แก้งานต่อ

ตรวจเทียบ ReportController, ReportServiceImpl, ReportQueryRepository และ ReportsPage ณ commit `131305f` วันที่ 9 ตุลาคม 2026; เป็น contract ที่ตรวจจาก source ไม่ใช่หลักฐานผลรันของ deployment

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

ทั้งสาม endpoint รับ `from`, `to`, `clientId` และ `projectId` แบบ optional:

| Parameter | รูปแบบ | ความหมาย |
|---|---|---|
| `from` และ `to` | `YYYY-MM-DD` | ส่งทั้งคู่เพื่อดูช่วงวันที่ โดยรวมวันเริ่มและวันสิ้นสุดตามเขตเวลา Asia/Bangkok; เว้นทั้งคู่เพื่อดูทุกช่วงเวลา |
| `clientId` | UUID | จำกัดข้อมูลเฉพาะลูกค้า |
| `projectId` | UUID | จำกัดข้อมูลเฉพาะโปรเจกต์ |

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

`/summary` คืน `data.generatedAt`, `data.filters.clients`, `data.filters.projects` และ `data.summary` ซึ่งมี `totalTrackedSeconds`, `trackedTimeTrendPercent`, `timeEntryCount`, `projectsWithTime`, `totalProjects`, `clientsWithTime`, `totalClients` ส่วน `/distribution` คืน `data.groupBy` และ `data.items` ที่มี `id`, `name`, `trackedSeconds`, `percent`

`trackedTimeTrendPercent` เป็น `null` เมื่อดูทุกช่วงเวลา เพราะไม่มีช่วงก่อนหน้าให้เทียบ `targetSeconds` และ `usagePercent` เป็น `null` เมื่อโปรเจกต์ไม่มีเป้าหมาย ชั่วโมงที่ใช้คิดจากวินาทีที่ Backend ส่งมา และความคืบหน้างานคิดจากจำนวน task ที่เสร็จต่อ task ทั้งหมด

เมื่อระบุวันที่ trend เทียบช่วงก่อนหน้าที่ติดกันและยาวเท่ากัน ใช้ `(current-previous)/previous*100`; หากยอดช่วงก่อนเป็น 0 ให้ trend เป็น null เช่นกัน taskProgressPercent นับเฉพาะ Task ที่ active และไม่ถูก soft delete; ไม่มี Task ให้เปอร์เซ็นต์เป็น 0

## จุดที่ต้องระวัง

- ข้อมูลทุก query ถูกจำกัดด้วย owner จาก access token; ยอดเวลาไม่รวม Time Entry ที่ยังไม่จบ/inactive/ถูก soft delete หรือ Project/Client ที่ถูก soft delete แต่ยังรวม Client/Project ที่ archive และเวลาเดิมบน Task ที่ถูก soft delete เพราะ time query ไม่กรอง Task
- ตัวเลือก Client/Project และจำนวนทั้งหมดไม่กรองตามช่วงวันที่; วันที่จำกัดยอดเวลาและจำนวนรายการที่มีเวลา ไม่ได้ซ่อน Project ที่ไม่มีเวลาในช่วงนั้น
- ยอด Reports ไม่จำเป็นต้องเท่ากับ Dashboard/Client GET: ดู [ตารางขอบเขตยอดเวลา](use-case-description.md#ตารางขอบเขตยอดเวลา) ก่อนเทียบตัวเลข
- Backend รวมยอดด้วย query แบบ aggregate ไม่ดึง time entry หรือ task ทีละโปรเจกต์
- ตารางและกราฟเปรียบเทียบโปรเจกต์แสดงเฉพาะหน้า pagination ปัจจุบัน ปุ่ม CSV จึงส่งออกเฉพาะหน้านั้น
- `getProjects` ใน Backend ยังโหลดโปรเจกต์ที่มองเห็นทั้งหมดเพื่อเรียงและตัดหน้าในหน่วยความจำ หากจำนวนโปรเจกต์มากควรย้าย pagination ไปที่ฐานข้อมูล
- ReportQueryRepository ใช้ JPQL ผ่าน EntityManager สำหรับยอดรวม และ native SQL ของ PostgreSQL สำหรับแบ่งวัน/ชั่วโมง ไม่ใช่ Spring Data derived query ทั้งหมด

## ตำแหน่งโค้ด

- Frontend: `code/Frontend/src/Analytics/pages/Reports/page.tsx`, `code/Frontend/src/services/report.ts`, `code/Frontend/src/types/analytics.ts`
- Backend: `code/Backend/src/main/java/th/ac/kku/freelance_hub/controller/ReportController.java`, `service/impl/ReportServiceImpl.java`, `repository/ReportQueryRepository.java`
