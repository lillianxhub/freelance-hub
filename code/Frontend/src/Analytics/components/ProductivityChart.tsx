import { CartesianGrid, Line, LineChart, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../components/ui/chart'
import { formatChartDuration } from '../../lib/dashboard'
import type { ProductivityChartProps } from '../../types/analytics'

function formatAxisDuration(totalSeconds: number): string {
  if (totalSeconds >= 3600) return `${Number((totalSeconds / 3600).toFixed(1))} ชม.`
  if (totalSeconds >= 60) return `${Number((totalSeconds / 60).toFixed(1))} นาที`
  return `${Math.round(totalSeconds)} วิ`
}

export default function ProductivityChart({ data }: ProductivityChartProps) {
  return (
    <ChartContainer className="h-72 w-full aspect-auto" config={{ totalSeconds: { label: 'เวลาที่บันทึก', color: 'var(--primary)' } }}>
      <LineChart data={[...data]} margin={{ top: 12, right: 12, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="day" tickLine={false} axisLine={false} minTickGap={24} />
        <YAxis tickLine={false} axisLine={false} width={64} tickFormatter={(value) => formatAxisDuration(Number(value))} />
        <ChartTooltip content={<ChartTooltipContent formatter={(value) => formatChartDuration(Number(value))} />} />
        <Line type="monotone" dataKey="totalSeconds" name="เวลาที่บันทึก" stroke="var(--color-totalSeconds)" strokeWidth={3} dot={{ r: 4, fill: 'var(--color-totalSeconds)', strokeWidth: 2 }} activeDot={{ r: 6 }} />
      </LineChart>
    </ChartContainer>
  )
}
