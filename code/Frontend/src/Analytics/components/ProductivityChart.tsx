import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { ProductivityChartProps } from '../../types/analytics'

export default function ProductivityChart({ data }: ProductivityChartProps) {
  return <ResponsiveContainer width="100%" height={255}><AreaChart data={[...data]}><defs><linearGradient id="totalHours" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#4F6BFF" stopOpacity={0.28} /><stop offset="95%" stopColor="#4F6BFF" stopOpacity={0.02} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" /><XAxis dataKey="day" tick={{ fontSize: 'var(--font-size-sm)' }} axisLine={false} tickLine={false} /><YAxis tick={{ fontSize: 'var(--font-size-sm)' }} axisLine={false} tickLine={false} /><Tooltip /><Area type="monotone" dataKey="total" name="เวลารวม" stroke="#4F6BFF" strokeWidth={2} fill="url(#totalHours)" /></AreaChart></ResponsiveContainer>
}
