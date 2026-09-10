# Use Case Description

รายละเอียด Use Case ต้องสอดคล้องกับ Use Case Diagram และ acceptance criteria ใน `REQUIREMENTS.md`

| Use Case | Actor | Preconditions | Main Flow | Alternative Flow | Postconditions |
|---|---|---|---|---|---|
| Track working time | Freelancer | เข้าสู่ระบบและมีโปรเจกต์ Active | เริ่ม timer แล้วหยุด timer | บันทึกเวลาย้อนหลัง | มี Time Entry ที่ถูกต้อง |
| Generate invoice | Freelancer | มี Billable Time Entry | เลือกรายการและออก Invoice | บันทึก Line Item เอง | ได้ Invoice และ PDF |
| View analytics | Freelancer | มีข้อมูลเวลา/รายได้ | เลือกช่วงเวลาและดู Dashboard | ไม่มีข้อมูลในช่วงที่เลือก | แสดง KPI หรือ Empty State |

