# Freelance Hub Frontend

Frontend ของ Freelance Hub พัฒนาด้วย React และ Vite ปัจจุบันใช้ mock data เป็นค่าเริ่มต้นเพื่อให้พัฒนาและสาธิตหน้าจอได้ระหว่างรอ Backend

## เริ่มต้นใช้งาน

```bash
npm install
npm run dev
```

เปิด `http://localhost:5173`

บัญชีสำหรับทดลอง:

- Email: `demo@freelancehub.test`
- Password: `demo1234`

สามารถสมัครบัญชีจำลองเพิ่มจากหน้า Register ได้ ข้อมูลจำลองถูกเก็บใน Local Storage ของเบราว์เซอร์และไม่ควรใช้เป็นระบบยืนยันตัวตนจริง

## เชื่อม Backend

Mock authentication เปิดเป็นค่าเริ่มต้น เมื่อต้องการเรียก Backend ที่ `http://localhost:8080` ให้สร้างไฟล์ `.env.local`:

```env
VITE_USE_MOCK_API=false
```

Dashboard ยังอ่านข้อมูลจาก `src/data/mockDashboardData.js` เมื่อ Backend analytics พร้อม ให้เปลี่ยนเฉพาะฟังก์ชัน `getDashboardData()` เป็นการเรียก API โดย component อื่นไม่จำเป็นต้องเปลี่ยน

## ตรวจสอบก่อนส่งงาน

```bash
npm run lint
npm run build
```
