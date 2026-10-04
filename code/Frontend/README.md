# Freelance Hub Frontend

React + TypeScript frontend สำหรับจัดการลูกค้า โปรเจกต์ งาน เวลา การเงิน Invoice และรายงาน ข้อมูล workspace ถูกบันทึกผ่าน Spring Boot backend พร้อม JWT ของผู้ใช้แต่ละคน

## เริ่มใช้งาน

1. ตั้งค่า PostgreSQL และค่าใน `code/Backend/.env` ตาม `.env.example`
2. จาก `code/Backend` รัน `bash ./mvnw spring-boot:run` (ค่าเริ่มต้นที่ port 8080)
3. จาก `code/Frontend` รัน `npm install` และ `npm run dev`
4. เปิด `http://localhost:5173` และสมัครบัญชีใหม่

กำหนด `VITE_API_BASE_URL` เป็น path บนโดเมนเว็บ เช่น `/api` ทั้ง Preview และ Production เพื่อให้ auth cookie ใช้ same-origin; ห้ามใส่ URL ข้ามโดเมนไว้ในค่านี้. Vite ใช้ path นี้เป็น proxy prefix และส่งคำขอไป backend ในเครื่องโดยอัตโนมัติ (หรือไปยัง `BACKEND_ORIGIN` เมื่อตั้งไว้). บน Vercel ให้ตั้ง `BACKEND_ORIGIN` เป็น HTTPS origin ของ Render (ไม่มี `/` ปิดท้าย) ทั้ง Preview และ Production; `vercel.ts` proxy path `/api` ไป backend.

การเข้าสู่ระบบใช้ `/api/auth/login`, การสมัครใช้ `/api/auth/register`, ต่ออายุ access token ผ่าน `/api/auth/refresh` และการตรวจผู้ใช้ใช้ `/api/users/me` Frontend เก็บ access token ในหน่วยความจำ; refresh token อยู่ใน HttpOnly cookie และไม่ได้เก็บใน Local Storage

ข้อมูลตัวอย่างเดิมที่อยู่ใน Local Storage หรือ Supabase จะไม่ถูกย้ายเข้าฐานข้อมูลใหม่นี้โดยอัตโนมัติ

Forgot/reset password และการอัปโหลดรูปโปรไฟล์ยังไม่อยู่ในขอบเขต MVP จึงไม่มี route หรือแบบฟอร์มสำหรับสองฟีเจอร์นี้

## ตรวจสอบ

```bash
npm test
npm run lint
npm run typecheck
npm run build
```

## โครงสร้าง

โครงสร้างปัจจุบันของ `src/` (ไม่มี `Workspace`, `Billing`, `Settings` หรือ `utils`):

```text
src/
├── main.tsx, App.tsx          # จุดเริ่มต้นและตัวแอป
├── routes/                    # กำหนดเส้นทางหน้าเว็บ
├── Authentication/            # เข้าสู่ระบบ สมัครสมาชิก และการป้องกัน route
├── Profile/                   # ข้อมูลผู้ใช้และการแก้ไขโปรไฟล์
├── ClientManagement/          # รายการและรายละเอียดลูกค้า
├── ProjectManagement/         # โปรเจกต์และงาน
├── TimeTracking/              # จับเวลาและรายการเวลา
├── Analytics/                 # Dashboard, Reports และข้อมูลรายงานจำลอง
├── api/                       # HTTP client, token refresh และข้อผิดพลาด API
├── services/                  # ฟังก์ชันเรียก API และแปลงข้อมูลของแต่ละฟีเจอร์
├── shared/                    # hook ที่ใช้ร่วมกัน เช่น useAsyncData
├── lib/                       # ฟังก์ชันคำนวณ จัดรูปแบบ และแปลงข้อมูล
├── components/                # UI ร่วม รวมถึง components/ui
├── constants/                 # ค่าคงที่สำหรับการนำทาง
├── types/                     # ชนิดข้อมูลและ interface
├── assets/                    # ไฟล์ภาพที่ import จากโค้ด
└── styles/                    # CSS ของแอป (มี index.css ที่ราก src ด้วย)
```

โฟลเดอร์ฟีเจอร์มี `pages/` และ `components/` ตามที่ใช้งานจริง รวมถึง Context, hook และ validator ของฟีเจอร์นั้น ๆ ไฟล์ `*-context.ts` เก็บ Context object ส่วน `*Context.tsx` เก็บ Provider และ `use*.ts` เก็บ hook ที่อ่าน Context หรือโหลดข้อมูล

ไฟล์ทดสอบ `*.test.ts` อยู่ใกล้โค้ดที่ทดสอบใน `api/`, `services/` และ `lib/` ปัจจุบันคำสั่ง `npm test` ใน `package.json` ระบุเฉพาะ `api/`, `services/` และ `utils/` จึงยังไม่รวมชุดทดสอบใน `lib/`; ดูข้อจำกัดนี้เมื่ออ่านผลการทดสอบ

โค้ด TypeScript ไม่ใช้ `any` เมื่อระบุชนิดข้อมูลได้ หากรับค่าที่ไม่ทราบรูปแบบ ให้ใช้ `unknown` และตรวจชนิดก่อนนำไปใช้งาน
