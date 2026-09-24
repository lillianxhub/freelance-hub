// Mock data - จำลองรูปแบบเดียวกับที่ backend จะส่งกลับมาจริง
// พอ backend เสร็จ จะเปลี่ยนไฟล์นี้เป็นฟังก์ชัน fetch แทน

export const mockDashboardData = {
  greeting: "Petpinyo",
  trackedThisWeek: "28h 40m",
  trackedChange: "+12.4%",
  utilization: "84%",
  utilizationChange: "+6.2%",
  activeProjects: 4,
  tasksCompleted: "18/24",
  activityData: [
    { day: 'Mon', focus: 6, other: 1 },
    { day: 'Tue', focus: 7.5, other: 1 },
    { day: 'Wed', focus: 5, other: 1 },
    { day: 'Thu', focus: 7.5, other: 0.5 },
    { day: 'Fri', focus: 6.5, other: 1 },
    { day: 'Sat', focus: 2, other: 0.5 },
    { day: 'Sun', focus: 0, other: 0 },
  ],
  activeProjectsList: [
    { name: "Website redesign", client: "Northstar Studio", percent: 72, tasks: "12 of 16 tasks", status: "In progress" },
    { name: "Brand identity kit", client: "Good Goods Co.", percent: 48, tasks: "8 of 17 tasks", status: "In review" },
    { name: "Mobile app concept", client: "Orbit Labs", percent: 29, tasks: "4 of 14 tasks", status: "Planned" },
  ],
  upcomingTasks: [
    { title: "Finalize homepage concepts", project: "Website redesign · Today", status: "In progress" },
    { title: "Prepare logo review", project: "Brand identity kit · Tomorrow", status: "Review" },
    { title: "Map onboarding flow", project: "Mobile app concept · Sep 15", status: "Planned" },
  ]
}

// ฟังก์ชันจำลองการดึงข้อมูล (async เหมือน fetch จริง)
// พอ backend เสร็จ แค่แก้ฟังก์ชันนี้ให้เป็น fetch จริง ไม่ต้องแก้ไฟล์อื่นเลย
export async function getDashboardData() {
  // จำลอง delay เหมือนเรียก API จริง (0.5 วินาที)
  await new Promise((resolve) => setTimeout(resolve, 500))
  return mockDashboardData
}