import { FiBarChart2, FiBriefcase, FiClock, FiHome, FiUsers } from 'react-icons/fi'

export const workspaceLinks = [
  { to: '/dashboard', label: 'ภาพรวม', icon: FiHome },
  { to: '/projects', label: 'โปรเจกต์', icon: FiBriefcase },
  { to: '/time-tracker', label: 'บันทึกเวลา', icon: FiClock },
  { to: '/clients', label: 'ลูกค้า', icon: FiUsers },
]

export const manageLinks = [
  // { to: '/finances', label: 'การเงิน', icon: '฿' },
  // { to: '/invoices', label: 'Invoices', icon: '▤' },
  { to: '/reports', label: 'รายงาน', icon: FiBarChart2 },
]
