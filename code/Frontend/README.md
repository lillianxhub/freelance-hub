# Freelance Hub Frontend

React + TypeScript frontend สำหรับ Freelancer ใช้จัดการลูกค้า โปรเจกต์ งานย่อย เวลาทำงาน Dashboard และ Reports ข้อมูลถูกบันทึกผ่าน Spring Boot REST API ของแต่ละบัญชี MVP ยังไม่มี Invoice, Payment, forgot/reset password หรืออัปโหลดรูปโปรไฟล์ ดูภาพรวมโครงการและเกณฑ์รายวิชาใน [README หลัก](../../README.md) และ [REQUIREMENTS.md](../../REQUIREMENTS.md)

## เริ่มใช้งาน

ต้องมี Node.js และ npm ก่อน จาก root ของ repository ตั้งค่า Backend ตาม [Backend README](../Backend/README.md) แล้วเปิด Backend และ PostgreSQL:

```bash
cd code/Backend
cp .env.example .env
docker compose up --build
```

บน Windows PowerShell ใช้ `Copy-Item .env.example .env` แทน `cp` และแก้ `POSTGRES_PASSWORD` กับ `JWT_SECRET` ใน `.env` ก่อนรัน เปิดอีก terminal แล้วรัน Frontend:

```bash
cd code/Frontend
npm ci
npm run dev
```

เปิด `http://localhost:5173` แล้วสมัครบัญชีใหม่ Vite จะ proxy คำขอ `/api` ไป `http://localhost:8080` ตามค่าเริ่มต้น

## การเชื่อมต่อ API และ Auth

| ตัวแปร | หน้าที่ | ค่าแนะนำ |
| --- | --- | --- |
| `VITE_API_BASE_URL` | path ของ API บน origin เดียวกับเว็บ | `/api` |
| `BACKEND_ORIGIN` | origin ของ Backend ที่ Vite/Vercel proxy ไป | local: `http://localhost:8080`; Vercel: HTTPS origin ของ Render |

กำหนด `VITE_API_BASE_URL` เป็น path เช่น `/api` ทั้ง Preview และ Production เพื่อให้ auth cookie ใช้ same-origin และตั้ง `BACKEND_ORIGIN` บน Vercel เป็น HTTPS origin ที่ไม่มี `/` ปิดท้าย [vite.config.ts](vite.config.ts) จัดการ proxy ระหว่างพัฒนา ส่วน [vercel.ts](vercel.ts) จัดการ rewrite บน Vercel โดย `BACKEND_ORIGIN` เป็นค่าฝั่ง server ไม่ควรใช้ prefix `VITE_`

Frontend เรียก `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` และ `/api/users/me` Access JWT เก็บในหน่วยความจำ; refresh token อยู่ใน HttpOnly cookie เมื่อรีโหลดหน้า แอปใช้ refresh cookie เพื่อคืน session และเมื่อ API ตอบ `401` จะลองต่ออายุ access token ก่อนส่งคำขอซ้ำ ข้อมูล token ไม่ถูกเก็บใน Local Storage

การย้ายข้อมูลตัวอย่างเดิมจาก Local Storage หรือ Supabase เข้าฐานข้อมูล Backend ไม่เกิดขึ้นอัตโนมัติ

## หน้าจอหลัก

| Route | ใช้งาน |
| --- | --- |
| `/login`, `/register` | เข้าสู่ระบบและสมัครบัญชี |
| `/dashboard` | ภาพรวมการทำงานและเวลา |
| `/clients`, `/clients/:clientId` | รายการและรายละเอียดลูกค้า |
| `/projects`, `/projects/:projectId` | โปรเจกต์ งานย่อย และความคืบหน้า |
| `/time-tracker` | จับเวลาและจัดการรายการเวลา |
| `/reports` | สรุปและกรองข้อมูลเวลา พร้อมส่งออก CSV |
| `/profile` | ดูและแก้ไขโปรไฟล์ |

หน้าที่อยู่ภายใต้ workspace ต้องเข้าสู่ระบบก่อน รายละเอียด route อ้างอิง [AppRoutes.tsx](src/routes/AppRoutes.tsx)

## ตรวจสอบและ Build

จาก `code/Frontend`:

```bash
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

`npm test` รันชุดทดสอบตาม script ใน [package.json](package.json) ซึ่งครอบคลุม `src/api/*.test.ts`, `src/services/*.test.ts`, `src/utils/*.test.ts` และ `src/lib/*.test.ts` เท่านั้น; ไฟล์ทดสอบนอก path เหล่านี้ยังไม่ถูกรวมใน script ปัจจุบัน ผล build อยู่ใน `dist/` GitHub Actions ใช้ Node.js 22 และรัน lint, typecheck, tests, build เป็น `Frontend gate`

## โครงสร้าง

```text
src/
├── main.tsx, App.tsx          # จุดเริ่มต้นและตัวแอป
├── routes/                    # Route และ protected workspace
├── Authentication/            # Login, register, auth context
├── Profile/                   # ข้อมูลผู้ใช้
├── ClientManagement/          # รายการและรายละเอียดลูกค้า
├── ProjectManagement/         # โปรเจกต์และงานย่อย
├── TimeTracking/              # Timer และ time entries
├── Analytics/                 # Dashboard และ Reports
├── api/                       # HTTP client, token refresh, API errors
├── services/                  # เรียก Backend API และ map response
├── hooks/                     # React hooks ที่ใช้ข้ามฟีเจอร์
├── utils/                     # Helper ทั่วไป: CSV, วันที่, ระยะเวลา, ตัวเลข, ข้อความ
├── lib/                       # การคำนวณที่ผูกกับ domain ของระบบ
├── components/                # UI ที่ใช้ร่วมกัน รวม components/ui
├── types/                     # Domain และ API contracts
├── constants/                 # ค่าคงที่ของแอป
├── assets/                    # ภาพที่ import จากโค้ด
└── styles/                    # CSS ของแอป
```

ฟังก์ชัน generic อยู่ใน `utils/`; business logic ที่ใช้ข้ามฟีเจอร์อยู่ใน `lib/`; helper เฉพาะฟีเจอร์อยู่ใกล้หน้าหรือ component ที่ใช้ เช่น `TimeTracking/timeTracking.utils.ts` และ React hook ที่ใช้ร่วมกันอยู่ใน `hooks/` ไฟล์ Context ชื่อพิมพ์เล็กเก็บ Context object ส่วน `*Context.tsx` เก็บ Provider

## Deploy

Vercel ใช้ Root Directory `code/Frontend`, Build Command `npm run build` และ Output Directory `dist` ตั้ง `VITE_API_BASE_URL=/api` และ `BACKEND_ORIGIN` ให้ตรงกับ Backend ของ environment นั้น ๆ เมื่อเปลี่ยน `BACKEND_ORIGIN` ต้อง redeploy เพื่อสร้าง rewrite ใหม่ Frontend deployment ใช้ Vercel Git integration; URL ที่กำหนดไว้ในโครงการอยู่ใน [README หลัก](../../README.md#deployment-url)
