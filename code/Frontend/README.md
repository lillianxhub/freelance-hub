# Freelance Hub Frontend

React + TypeScript frontend สำหรับจัดการลูกค้า โปรเจกต์ งาน เวลา การเงิน Invoice และรายงาน ข้อมูล workspace ถูกบันทึกผ่าน Spring Boot backend พร้อม JWT ของผู้ใช้แต่ละคน

## เริ่มใช้งาน

1. ตั้งค่า PostgreSQL และค่าใน `code/Backend/.env` ตาม `.env.example`
2. จาก `code/Backend` รัน `bash ./mvnw spring-boot:run` (ค่าเริ่มต้นที่ port 8080)
3. จาก `code/Frontend` รัน `npm install` และ `npm run dev`
4. เปิด `http://localhost:5173` และสมัครบัญชีใหม่

Vite ส่งคำขอ `/api` ไปที่ backend ในเครื่องโดยอัตโนมัติ หากใช้ reverse proxy หรือ URL อื่นให้ตั้ง `VITE_API_BASE_URL` ตาม `.env.example` และให้ backend อนุญาต origin นั้น

การเข้าสู่ระบบใช้ `/api/auth/login`, การสมัครใช้ `/api/auth/register`, และการตรวจ session ใช้ `/api/users/me` หน้าอื่นใช้ `/api/workspace/{resource}` เพื่ออ่าน บันทึก และลบข้อมูลของบัญชีปัจจุบัน backend ใช้ตาราง `workspace_records` จาก Flyway migration V7 และกำหนด `owner_id` จาก JWT เสมอ

ข้อมูลตัวอย่างเดิมที่อยู่ใน Local Storage หรือ Supabase จะไม่ถูกย้ายเข้าฐานข้อมูลใหม่นี้โดยอัตโนมัติ

API ตั้งรหัสผ่านใหม่ยังไม่มีใน backend หน้าลืมรหัสผ่านจะแสดงสถานะนี้ให้ผู้ใช้ทราบ

## ตรวจสอบ

```bash
npm test
npm run lint
npm run typecheck
npm run build
```

## โครงสร้าง

`src/Authentication`, `src/Workspace`, `src/ClientManagement`, `src/ProjectManagement`, `src/TimeTracking`, `src/Analytics`, `src/Billing` และ `src/Settings` แยก Context, hooks, components และ pages ตามหน้าที่ ส่วนโค้ดที่ใช้ร่วมกันอยู่ในโฟลเดอร์กลาง:

- `src/api` เก็บ fetch wrapper และ HTTP helpers
- `src/services` เก็บการเรียก backend และการจัดการข้อมูลจาก API
- `src/utils` เก็บฟังก์ชันคำนวณ จัดรูปแบบ และแปลงข้อมูล
- `src/components` เก็บ UI ที่ใช้ร่วมกันหลายฟีเจอร์
- `src/types` เก็บ type และ interface โดยแยกไฟล์ตามข้อมูลหรือหน้าที่ใช้งาน

โค้ด TypeScript ไม่ใช้ `any` เมื่อระบุชนิดข้อมูลได้ หากรับค่าที่ไม่ทราบรูปแบบ ให้ใช้ `unknown` และตรวจชนิดก่อนนำไปใช้งาน
