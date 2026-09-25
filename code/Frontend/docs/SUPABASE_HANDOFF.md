# Supabase backend handoff

เอกสารนี้อธิบายจุดเชื่อมระหว่าง React frontend และ Supabase เพื่อให้แก้ backend โดยไม่กระทบ UI

## จุดเชื่อมหลัก

- `src/types/domain.ts` เป็น contract กลางของข้อมูลที่ UI และ repository ใช้ร่วมกัน
- `src/lib/supabase.ts` สร้าง Supabase client และเลือก Demo/Supabase mode
- `src/services/authService.ts` รวม Auth operations
- `src/services/workspaceRepository.ts` รวม CRUD ของ business resources
- `src/contexts/WorkspaceContext.tsx` โหลดข้อมูลและ expose `save`, `remove`, `refresh`
- `supabase/migrations/202609250001_initial_schema.sql` คือ schema ที่ frontend คาดหวัง

Component และ page ไม่ควร import Supabase client โดยตรง หากเปลี่ยนวิธีเชื่อม backend ให้แก้ใน service layer นี้

## Environment contract

| Variable | ใช้ทำอะไร |
|---|---|
| `VITE_SUPABASE_URL` | Supabase project URL |
| `VITE_SUPABASE_PUBLISHABLE_KEY` | Publishable/anon key สำหรับ browser |
| `VITE_DEMO_MODE` | ต้องเป็น `false` เพื่อใช้ Supabase |

Frontend ส่ง access token ผ่าน Supabase client อัตโนมัติ RLS ใช้ `auth.uid()` ตรวจ owner ไม่ต้องส่ง service-role key หรือ trusted `owner_id` จากระบบอื่น

## Data resources

| Resource | ความสัมพันธ์หลัก | Frontend usage |
|---|---|---|
| `profiles` | one-to-one กับ Auth user | seller details, defaults, settings |
| `clients` | owner has many | client CRUD และ invoice recipient |
| `projects` | belongs to client | billing rule, rate, budget |
| `tasks` | belongs to project | task CRUD/reorder/status |
| `time_entries` | belongs to project/task, optional invoice | timer, manual time, billing snapshot |
| `finance_entries` | optional project | other income/expense |
| `invoices` | belongs to client, optional project | totals, status, seller/client snapshots |
| `invoice_items` | belongs to invoice, optional time entry | calculated/manual lines |
| `payments` | belongs to invoice | partial/full payment history |

ทุก record มี `owner_id`; repository ใส่ค่า user ปัจจุบันก่อน upsert และ RLS ตรวจซ้ำในฐานข้อมูล

## Business rules enforced by database

- ผู้ใช้เห็นและแก้เฉพาะ record ของตนเองผ่าน RLS
- มี running time entry ได้สูงสุดหนึ่งรายการต่อ owner
- due date ต้องไม่ก่อน issue date
- duration และจำนวนเงินต้องเป็นค่าที่ถูกต้องตาม constraints
- time entry ใช้ซ้ำไม่ได้ใน Invoice ที่ไม่ใช่ `VOID`
- Invoice ที่ออกแล้วแก้เลขที่ คู่สัญญา วันที่ ยอด ภาษี รายการ หรือ snapshot ไม่ได้
- ลบได้เฉพาะ Draft และสถานะเปลี่ยนได้ตาม Draft → Issued → Paid/Overdue/Void
- เพิ่ม แก้ หรือลบ line items ได้เฉพาะตอน Invoice เป็น Draft
- Auth signup trigger สร้าง `profiles` ให้อัตโนมัติ
- foreign keys ป้องกัน orphan records และรักษาประวัติธุรกรรม

## Invoice write flow

Frontend ทำงานตามลำดับ:

1. สร้าง/อัปเดต `invoices` สถานะ `DRAFT`
2. เขียน `invoice_items`
3. อัปเดต `time_entries.invoice_id`
4. เมื่อ Issue ให้เปลี่ยนสถานะและบันทึก `issued_at`
5. เมื่อรับชำระให้เพิ่ม `payments` แล้วอัปเดต `amount_paid`/`status`
6. เมื่อ Void ให้คืน `time_entries.invoice_id` เป็น `null`

สำหรับ production ที่มีหลาย request พร้อมกัน แนะนำย้ายขั้นตอนสร้าง/แก้ Invoice ไปเป็น Postgres RPC หรือ Edge Function transaction แล้วให้ `workspaceRepository` เรียกฟังก์ชันนั้น จุดอื่นของ UI ไม่ต้องเปลี่ยน

## Security checklist

- ใช้ Publishable key ใน browser เท่านั้น
- ไม่ commit `.env.local`
- ทดสอบผู้ใช้ A ไม่เห็นข้อมูลผู้ใช้ B
- เปิด RLS ทุก business table ก่อน deploy
- ตรวจ Auth redirect URLs ใน Supabase dashboard
- หากเพิ่ม table ใหม่ ให้เพิ่ม grants/policies และเพิ่มชื่อ resource ใน repository

## Smoke test หลังเชื่อมจริง

1. สมัครผู้ใช้ใหม่และตรวจว่ามี profile หนึ่งแถว
2. สร้าง Client → Project → Task
3. Start timer, refresh หน้า, แล้ว Stop
4. สร้าง Draft จาก unbilled time และตรวจว่าเลือกเวลาซ้ำไม่ได้
5. Issue → บันทึก partial payment → full payment
6. Sign in ด้วยผู้ใช้อื่นและยืนยันว่าไม่เห็นข้อมูลชุดแรก
