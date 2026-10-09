# SOLID Analysis: Dashboard/Analytics และ Frontend

**เจ้าของ feature:** `nattadol_673380511-9_02`  
**ขอบเขต:** Dashboard/Reports API และหน้า React ที่แสดงผล รวม summary queries, KPI, กราฟและตาราง ไม่รวมการพัฒนา Timer, Project/Task หรือ Client API ซึ่งเป็นงาน feature อื่น

เอกสารนี้อ้างอิง implementation ปัจจุบันภายใต้ `code/Backend/src/main/java/th/ac/kku/freelance_hub/` และ `code/Frontend/src/` โดยอ้างชื่อไฟล์และฟังก์ชันแทนเลขบรรทัดที่เปลี่ยนได้หลัง merge

| Principle | หลักฐานในโค้ด | เหตุผลและขอบเขต |
|---|---|---|
| Single Responsibility | `DashboardController`, `ReportController`, `DashboardServiceImpl`, `ReportServiceImpl`, `ReportQueryRepository` | Controller รับ HTTP, service สรุปผลตามกติกา Analytics, repository ทำ aggregate query; ไม่คำนวณ KPI ใน controller |
| Single Responsibility | `Analytics/DashboardContext.tsx`, `Analytics/useDashboard.ts`, `Analytics/pages/Dashboard/page.tsx` | Context โหลดข้อมูลและให้ `refresh/loadActivity`; hook ให้ทางเข้าใช้งาน; page จัด state ของช่วงกราฟและประกอบ UI |
| Single Responsibility | `Analytics/components/ProductivityChart.tsx`, `Analytics/dashboardChart.ts` | Component วาดกราฟ Recharts ส่วน utility แปลงข้อมูลจุดกราฟและจัดหน่วยเวลา จึงทดสอบกติกาแสดงผลได้แยกจาก DOM |
| Single Responsibility | `services/report.ts`, `Analytics/pages/Reports/page.tsx`, `types/analytics.ts` | Service สร้าง URL และอ่าน response, page จัด filter/loading/การแสดงผล, type ระบุรูปข้อมูลที่ใช้ |
| Single Responsibility | `Analytics/analytics.utils.ts` (`toCsv`, `downloadCsv`) | ReportsPage เรียก utility เพื่อสร้าง CSV แทนการประกอบไฟล์ใน markup; `summarizeTime`, `groupTimeBy` และ `inDateRange` มีอยู่ในไฟล์ แต่หน้า Reports ปัจจุบันไม่ได้เรียก |
| Open/Closed | `SummaryCard`, `Card`, `Table`, `ChartContainer` ที่ Dashboard และ Reports ใช้ | แต่ละหน้าประกอบ component ที่มีอยู่เพื่อเพิ่ม card/กราฟโดยไม่ต้องแก้โครงสร้าง UI กลาง; การเพิ่ม endpoint/รูปกราฟใหม่ยังต้องแก้ page และ type |
| Open/Closed | `DashboardActivity` กับ `DashboardChartPeriod` ใน `types/dashboard.ts` | `WEEK/MONTH/YEAR` เป็นชุดค่าที่กำหนดชัด กราฟแปลงข้อมูลผ่าน `dashboardChart.ts`; ไม่ได้ใช้ GoF Strategy และการเพิ่ม period ใหม่ต้องแก้ type, utility และ Backend |
| Liskov Substitution | `DashboardService`/`DashboardServiceImpl`, `ReportService`/`ReportServiceImpl` | Controller เรียกตาม service contract โดยไม่อ้าง implementation ตรง ๆ; แต่แต่ละ interface มี production implementation เดียว จึงยังไม่พิสูจน์ว่าทดแทนกันได้ทุกพฤติกรรม |
| Interface Segregation | `DashboardService` และ `ReportService` เป็นคนละ contract; `types/dashboard.ts`/`types/analytics.ts` แยกข้อมูลตามหน้าและ component | หน้า Dashboard ไม่ต้องพึ่ง Reports API; component เช่น `DashboardTimerCard` รับเฉพาะ props ที่ใช้ ไม่รับ response รายงานทั้งชุด |
| Dependency Inversion | `DashboardController` พึ่ง `DashboardService`, `ReportController` พึ่ง `ReportService`; `DashboardPage` เรียก `useDashboard()`, `ReportsPage` เรียก `services/report.ts` | Controller พึ่ง interface ฝั่ง Backend และ React page ไม่ประกอบ `fetch` เอง; Frontend service ยังเป็น concrete function ไม่ใช่ interface ที่สลับ implementation ได้โดยอัตโนมัติ |

## หลักฐานตามการทำงานจริง

- `DashboardProvider` เรียก `GET /api/dashboard` สำหรับ KPI, งาน, โปรเจกต์และรายการเวลาล่าสุด; `loadActivity(period)` รองรับ `WEEK/MONTH/YEAR` แต่หน้า Dashboard เรียกเพิ่มเฉพาะเดือนและปี กราฟสัปดาห์ใช้ `dailyWork` จาก response หลัก
- `DashboardPage` อ่าน current timer ผ่าน `useCurrentTimer()` แล้วส่งเป็น props ให้ `DashboardTimerCard`; card แสดง running timer ก่อน หากไม่มีจึงใช้ `recentTimeEntries` จาก dashboard response และรองรับ task หรือรายการล่าสุดที่ไม่มีชื่องาน
- เมื่อหยุด timer จากการ์ด Dashboard, page เรียก action ที่ feature Time Tracking มีอยู่ แล้ว refresh current timer และ dashboard; งาน Analytics รับผิดชอบเฉพาะการแสดงผลและการเชื่อม action นี้
- `ReportsPage` ใช้ `getReportSummary`, `getReportDistribution` และ `getReportProjects`; service ส่งเฉพาะ filter ที่เลือก และดึง pagination จาก `meta` ของ `/projects`
- รายงานไม่ส่งวันที่เมื่อเว้นว่างทั้งคู่; หากกรอกช่วงไม่ครบหรือย้อนวันที่ หน้าไม่เรียก API และแสดง error; ผู้ใช้เลือก distribution แบบ `CLIENT` หรือ `PROJECT` ได้เอง
- ตาราง, กราฟเปรียบเทียบโปรเจกต์ และ CSV อ้างอิงโปรเจกต์จากหน้า pagination ปัจจุบัน; จึงไม่อ้างว่า CSV เป็นข้อมูลทุกหน้า
- ตัวเลขเวลาเก็บเป็นวินาทีจนถึงชั้นแสดงผล `dashboardChart.ts` และ `lib/formatters.ts` จึงไม่ทำให้เวลาต่ำกว่าหนึ่งชั่วโมงหายไปจากกราฟ

## ข้อจำกัดของการวิเคราะห์

1. OCP และ LSP เป็นจุดต่อขยาย/contract เชิงโครงสร้าง ไม่ได้พิสูจน์ว่ามี implementation หลายตัวแทนกันได้จริง
2. `ReportsPage` ยังรวม state ของตัวกรอง, การเรียกสามชุดข้อมูล, การสร้างข้อมูลกราฟและ CSV ใน page เดียว; หากซับซ้อนขึ้นควรแยก hook ตามความรับผิดชอบ
3. `services/report.ts` มีฟังก์ชัน `getReportWorkTrend` และ `getReportWorkPattern` แต่หน้า Reports ปัจจุบันไม่ได้เรียก ไม่ควรนับเป็นส่วน UI ที่เสร็จแล้ว
4. Test ที่มีคือ `Analytics/dashboardChart.test.ts`, `Analytics/dashboard.test.tsx`, `services/report.test.ts` และ `Analytics/analytics.utils.test.ts`; การทดสอบเหล่านี้ตรวจ utility, การโหลด Dashboard และ contract ของ report service แต่ยังไม่ครอบคลุมการกดสลับกราฟบนหน้า Reports โดยตรง
