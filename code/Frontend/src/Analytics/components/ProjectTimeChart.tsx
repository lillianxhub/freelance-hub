import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { ProjectTimeChartProps } from '../../types/analytics'

export default function ProjectTimeChart({ data }: ProjectTimeChartProps) {
  if (!data.length) return <p className="inline-empty">ไม่มีข้อมูลในช่วงวันที่นี้</p>
  return <ResponsiveContainer width="100%" height={270}><BarChart data={[...data]}><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8ebf1" /><XAxis dataKey="name" tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><YAxis tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><Tooltip /><Bar dataKey="hours" name="ชั่วโมง" fill="#3867f4" radius={[5, 5, 0, 0]} /></BarChart></ResponsiveContainer>
}
