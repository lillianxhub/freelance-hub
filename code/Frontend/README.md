# Freelance Hub Frontend

React frontend สำหรับจัดการลูกค้า โปรเจกต์ Task เวลา รายรับ–รายจ่าย Invoice และรายงานของ Freelancer ออกแบบเป็น light workspace ตาม mockup ของทีม และรองรับมือถือ แท็บเล็ต และเดสก์ท็อป

ระบบทำงานได้ 2 โหมด:

- Demo mode: ใช้ข้อมูลตัวอย่างใน Local Storage สำหรับพัฒนา UI และสาธิตโดยไม่ต้องมี backend
- Supabase mode: ใช้ Supabase Auth, PostgreSQL และ Row Level Security จริง โดยไม่ต้องแก้ component

## ฟีเจอร์ที่พร้อมใช้งาน

- สมัคร เข้าสู่ระบบ ออกจากระบบ และขอ reset password
- Client CRUD, search, status filter, pagination, archive และหน้ารายละเอียด
- Project/Task CRUD, reorder, status, hourly/fixed price และ budget alerts
- Timer ที่ทำงานได้ครั้งละหนึ่งรายการ, manual entry, edit/delete/duplicate และ filters
- Income/expense พร้อมสรุป cash flow
- Invoice จาก unbilled time หรือ manual items, tax/discount, Draft edit, Issue, partial/full payment, Void และพิมพ์เป็น PDF
- Dashboard วันนี้/สัปดาห์/เดือน, utilization, revenue states, project progress และ upcoming tasks
- Reports ตามช่วงวันที่/สกุลเงิน, project/client breakdown, effective hourly rate และ CSV exports
- Profile, invoice seller/bank details, timezone, currency, date format, default rate และ tax
- Loading, empty และ error states ทุกกลุ่มหน้าหลัก

## เริ่มใช้งานแบบ Demo

ต้องมี Node.js 20 ขึ้นไป จากโฟลเดอร์ `code/Frontend` รัน:

```bash
npm install
npm run dev
```

เปิด <http://localhost:5173> และใช้บัญชี:

- Email: `demo@freelancehub.test`
- Password: `demo1234`

Demo mode เป็นค่าเริ่มต้น ข้อมูลอยู่ใน Local Storage และกดคืนข้อมูลเริ่มต้นได้ที่ Settings

## เชื่อม Supabase

1. สร้าง Supabase project
2. เปิด SQL Editor แล้วรัน `supabase/migrations/202609250001_initial_schema.sql`
3. เปิด Email/Password provider ที่ Authentication
4. คัดลอก `.env.example` เป็น `.env.local`
5. ใส่ค่า Project URL และ Publishable key จาก Supabase

```env
VITE_SUPABASE_URL=https://YOUR_PROJECT.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=YOUR_PUBLISHABLE_KEY
VITE_DEMO_MODE=false
```

รีสตาร์ต dev server หลังแก้ environment variables ห้ามนำ `service_role` key ใส่ frontend เพราะ key นี้ข้าม Row Level Security ได้

Schema สร้าง 9 ตาราง, foreign keys, indexes, validation constraints, trigger สร้าง profile หลังสมัคร, one-running-timer guard, duplicate invoice-time guard และ RLS ที่บังคับ `owner_id = auth.uid()` แล้ว

ดูรายละเอียดสำหรับส่งต่อ backend ที่ [Supabase handoff](docs/SUPABASE_HANDOFF.md)

## Routes

| Route | หน้าที่ |
|---|---|
| `/login`, `/register`, `/forgot-password` | Authentication |
| `/dashboard` | ภาพรวมและ analytics |
| `/clients`, `/clients/:clientId` | ลูกค้า |
| `/projects`, `/projects/:projectId` | โปรเจกต์และ Task |
| `/time-tracker` | Timer และ time entries |
| `/finances` | รายรับและค่าใช้จ่าย |
| `/invoices`, `/invoices/new` | รายการและสร้าง Invoice |
| `/invoices/:invoiceId`, `/invoices/:invoiceId/edit` | ดูและแก้ไข Draft |
| `/reports` | Analytics และ CSV |
| `/settings` | Profile และค่าเริ่มต้น |

## ตรวจสอบก่อนส่งงาน

```bash
npm test
npm run lint
npm run build
```

Automated tests ตรวจสูตรเวลาทำงาน, utilization, invoice balance/overdue และ CSV escaping ส่วน production build อยู่ใน `dist/` ซึ่งไม่ถูก commit

## โครงสร้างสำคัญ

```text
src/
├── components/       shared UI และ app shell
├── contexts/         auth/workspace state
├── data/             demo seed data
├── lib/              Supabase client
├── pages/            route-level screens
├── services/         auth และ data repository abstraction
└── utils/            calculations, formatting และ tests
supabase/
└── migrations/       PostgreSQL schema + RLS
```

UI ไม่เรียก Supabase โดยตรง ทุกหน้าใช้ `workspaceRepository` ผ่าน `WorkspaceContext` ทำให้เพื่อน backend เปลี่ยน data implementation หรือเพิ่ม RPC ได้โดยไม่ต้องแก้ทุกหน้า
