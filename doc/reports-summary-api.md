# Reports Summary API Contract

เอกสารนี้เป็นข้อตกลงระหว่าง Frontend และ Backend สำหรับหน้า **รายงาน** รวมถึงเป็นบริบทให้ AI แก้โค้ดต่อโดยไม่กลับไปเรียก API หลายเส้น

## เป้าหมาย

หน้า Reports ต้องใช้ข้อมูลจาก endpoint เดียว:

```http
GET /api/reports/summary?from=2026-10-01&to=2026-10-31&clientId={uuid}&projectId={uuid}
```

ห้ามประกอบรายงานด้วยการดึง `/api/clients`, `/api/projects`, `/api/time-entries` ทุกหน้า หรือเรียก `/api/projects/{projectId}/tasks` แยกตามโปรเจกต์ เพราะทำให้เกิด request จำนวนมากและปัญหา N+1

## Query parameters

| Parameter | Required | รูปแบบ | ความหมาย |
|---|---:|---|---|
| `from` | ใช่ | `YYYY-MM-DD` | วันเริ่มต้นแบบรวมวันนั้น โดยใช้เขตเวลา Asia/Bangkok |
| `to` | ใช่ | `YYYY-MM-DD` | วันสิ้นสุดแบบรวมวันนั้น โดยใช้เขตเวลา Asia/Bangkok |
| `clientId` | ไม่ | UUID | จำกัดข้อมูลเฉพาะลูกค้า |
| `projectId` | ไม่ | UUID | จำกัดข้อมูลเฉพาะโปรเจกต์ |

เมื่อไม่เลือกตัวกรอง Frontend จะไม่ส่ง `clientId` หรือ `projectId` และจะไม่ส่งคำว่า `ALL`

## Response

Response ชั้นนอกใช้รูปแบบกลางของระบบ:

```json
{
  "success": true,
  "message": "ดึงข้อมูลรายงานสำเร็จ",
  "data": {
    "generatedAt": "2026-10-04T10:00:00Z",
    "filters": {
      "clients": [
        { "id": "client-uuid", "name": "Northstar Studio" }
      ],
      "projects": [
        { "id": "project-uuid", "name": "Website Redesign", "clientId": "client-uuid" }
      ]
    },
    "summary": {
      "totalTrackedSeconds": 100800,
      "trackedTimeTrendPercent": 12.5,
      "timeEntryCount": 42,
      "projectsWithTime": 4,
      "totalProjects": 6,
      "clientsWithTime": 3,
      "totalClients": 4
    },
    "timeByClient": [
      {
        "clientId": "client-uuid",
        "clientName": "Northstar Studio",
        "trackedSeconds": 100800,
        "percent": 37.0
      }
    ],
    "projectUsage": [
      {
        "projectId": "project-uuid",
        "projectName": "Website Redesign",
        "clientId": "client-uuid",
        "clientName": "Northstar Studio",
        "color": "#4F6BFF",
        "targetSeconds": 144000,
        "trackedSeconds": 100800,
        "usagePercent": 70.0,
        "taskProgressPercent": 75.0,
        "status": "ACTIVE"
      }
    ]
  },
  "meta": null,
  "error": null
}
```

## ความหมายและสูตรคำนวณ

- นับเฉพาะ Time Entry ที่จับเวลาเสร็จแล้วและไม่ถูกลบ
- `totalTrackedSeconds` คือผลรวมเวลาในช่วงและตัวกรองที่ขอ
- `trackedTimeTrendPercent` เทียบกับช่วงก่อนหน้าที่มีจำนวนวันเท่ากัน สูตร `(ช่วงปัจจุบัน - ช่วงก่อนหน้า) / ช่วงก่อนหน้า * 100`
- `projectsWithTime` และ `clientsWithTime` นับเฉพาะรายการที่มีเวลามากกว่า 0 ในช่วงที่เลือก
- `totalProjects` และ `totalClients` คือจำนวนทั้งหมดภายใต้ตัวกรองปัจจุบัน แม้ไม่มีการบันทึกเวลา
- `usagePercent` คำนวณจาก `trackedSeconds / targetSeconds * 100`; ส่ง `null` เมื่อไม่มีเป้าหมาย และอาจเกิน 100
- `taskProgressPercent` คำนวณจาก `completedTasks / totalTasks * 100`; เมื่อไม่มีงานให้เป็น 0
- ค่าระยะเวลาทั้งหมดใช้หน่วยวินาที Frontend มีหน้าที่จัดรูปแบบเพื่อแสดงผลเท่านั้น

## Mock ที่ใช้อยู่

- Contract TypeScript: `code/Frontend/src/types/analytics.ts`
- Mock response และการกรอง: `code/Frontend/src/Analytics/reports.mock.ts`
- Service ที่หน้า Reports เรียก: `code/Frontend/src/services/report.ts`
- UI: `code/Frontend/src/Analytics/pages/Reports/page.tsx`

ขณะนี้ `getReportSummary()` คืนข้อมูลจาก mock ในเครื่องและไม่ยิง network request

## การเปลี่ยนไปใช้ Backend จริง

เมื่อ Backend พร้อม ให้แก้เฉพาะ `code/Frontend/src/services/report.ts` ให้สร้าง `URLSearchParams` จาก query แล้วเรียก:

```ts
const response = await api.get<ReportSummaryData>(`/reports/summary?${params.toString()}`)
return response.data
```

ไม่ต้องเปลี่ยน component หน้า Reports หาก response ตรงตาม contract นี้ และควรเพิ่ม test ยืนยันว่า request มี `from`, `to` และส่ง optional filter เฉพาะเมื่อมีค่า

## ข้อกำหนด Backend

- ตรวจสอบว่า `from` ไม่เกิน `to`
- จำกัดข้อมูลด้วย owner จาก access token ทุก query
- รวมข้อมูลด้วย query แบบ aggregate หรือจำนวน query คงที่ ห้าม query task/time entry ภายใน loop ของแต่ละโปรเจกต์
- คืน `filters` ใน response เดียวเพื่อให้ dropdown ไม่ต้องเรียก Clients และ Projects API เพิ่ม
- ใช้ response wrapper กลางและไม่คืน JPA entity โดยตรง
