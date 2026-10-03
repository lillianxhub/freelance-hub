# Dashboard API contract

Dashboard ต้องโหลดข้อมูลทั้งหมดด้วย request เดียว:

```http
GET /api/dashboard?from=2026-09-25&to=2026-10-01&recentTimeEntryLimit=2&activeProjectLimit=5&pendingTaskLimit=5
```

- `from` และ `to` เป็นวันที่รูปแบบ `YYYY-MM-DD` และใช้คำนวณกราฟรายวัน
- `recentTimeEntryLimit` ค่าเริ่มต้น `2`
- `activeProjectLimit` ค่าเริ่มต้น `5`
- `pendingTaskLimit` ค่าเริ่มต้น `5`
- โปรเจกต์ต้องเป็นสถานะ `ACTIVE`
- งานต้องไม่เป็นสถานะ `DONE` และเรียงกำหนดส่งใกล้ที่สุดก่อน
- รายการเวลาต้องเรียง `startedAt DESC`
- `changePercent` ของวันนี้เทียบเมื่อวาน และสัปดาห์นี้เทียบสัปดาห์ก่อน ให้ Backend คำนวณมาแล้ว

โครงสร้าง `data` ฉบับ TypeScript อยู่ใน `dashboard.contract.ts` ส่วน response ชั้นนอกใช้มาตรฐานเดิม:

```ts
interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  meta: null
  error: object | null
}
```

Frontend เตรียม `getDashboard(query)` ไว้ใน `dashboard.service.ts` แล้ว แต่ยังไม่เรียกใช้ระหว่างที่หน้า Dashboard ใช้ mock data ฟังก์ชันนี้ใช้ `api.get` เดิม จึงได้ base URL `/api`, access token, token refresh และ error handling จาก `apiClient.ts` อัตโนมัติ

การหยุดตัวจับเวลาไม่ต้องรวมเป็น method ใหม่ใน Dashboard API ให้ใช้ method เดิม:

```ts
import { stopTimer } from '../services/timeTracking'
await stopTimer() // POST /api/timer/stop
```

หลังหยุดสำเร็จ ให้เรียก `getDashboard()` ใหม่เพื่อ refresh Dashboard ทั้งชุด
