import { CartesianGrid, Line, LineChart, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../components/ui/chart'
import type { ProductivityChartProps } from '../../types/analytics'

export default function ProductivityChart({ data }: ProductivityChartProps) {
  return (
    <ChartContainer className="h-72 w-full aspect-auto" config={{ total: { label: 'ชั่วโมงที่บันทึก', color: 'var(--primary)' } }}>
      <LineChart data={[...data]} margin={{ top: 12, right: 12, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="day" tickLine={false} axisLine={false} minTickGap={24} />
        <YAxis tickLine={false} axisLine={false} width={32} tickFormatter={(value) => `${value}h`} />
        <ChartTooltip content={<ChartTooltipContent />} />
        <Line type="monotone" dataKey="total" name="ชั่วโมงที่บันทึก" stroke="var(--color-total)" strokeWidth={3} dot={{ r: 4, fill: 'var(--color-total)', strokeWidth: 2 }} activeDot={{ r: 6 }} />
      </LineChart>
    </ChartContainer>
  )
}
