import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { ProductivityChartProps } from '../../types/analytics'

export default function ProductivityChart({ data }: ProductivityChartProps) {
  return <ResponsiveContainer width="100%" height={255}><AreaChart data={[...data]}><defs><linearGradient id="totalHours" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#3867f4" stopOpacity={0.28} /><stop offset="95%" stopColor="#3867f4" stopOpacity={0.02} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8ebf1" /><XAxis dataKey="day" tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><YAxis tick={{ fontSize: 9 }} axisLine={false} tickLine={false} /><Tooltip /><Area type="monotone" dataKey="total" name="เวลารวม" stroke="#3867f4" strokeWidth={2} fill="url(#totalHours)" /><Area type="monotone" dataKey="billable" name="Billable" stroke="#20a176" strokeWidth={2} fill="transparent" /></AreaChart></ResponsiveContainer>
}
