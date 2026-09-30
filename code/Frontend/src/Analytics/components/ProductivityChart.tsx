import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../components/ui/chart'
import type { ProductivityChartProps } from '../../types/analytics'

export default function ProductivityChart({ data }: ProductivityChartProps) {
  return <ChartContainer className="h-[255px] w-full aspect-auto" config={{ total: { label: 'เวลารวม', color: '#4F6BFF' } }}><AreaChart data={[...data]}><defs><linearGradient id="totalHours" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#4F6BFF" stopOpacity={0.28} /><stop offset="95%" stopColor="#4F6BFF" stopOpacity={0.02} /></linearGradient></defs><CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" /><XAxis dataKey="day" tick={{ fontSize: 'var(--font-size-sm)' }} axisLine={false} tickLine={false} /><YAxis tick={{ fontSize: 'var(--font-size-sm)' }} axisLine={false} tickLine={false} /><ChartTooltip content={<ChartTooltipContent />} /><Area type="monotone" dataKey="total" name="เวลารวม" stroke="var(--color-total)" strokeWidth={2} fill="url(#totalHours)" /></AreaChart></ChartContainer>
}
