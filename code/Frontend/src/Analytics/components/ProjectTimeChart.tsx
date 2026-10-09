import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../components/ui/chart'
import type { ProjectTimeChartProps } from '../../types/analytics'

export default function ProjectTimeChart({ data }: ProjectTimeChartProps) {
  if (!data.length) return <p className="inline-empty">ไม่มีข้อมูลในช่วงวันที่นี้</p>
  return (
    <ChartContainer
      className="h-[270px] w-full aspect-auto"
      config={{ hours: { label: 'ชั่วโมง', color: '#4F6BFF' } }}
    >
      <BarChart data={[...data]}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
        <XAxis
          dataKey="name"
          tick={{ fontSize: 'var(--font-size-sm)' }}
          axisLine={false}
          tickLine={false}
        />
        <YAxis tick={{ fontSize: 'var(--font-size-sm)' }} axisLine={false} tickLine={false} />
        <ChartTooltip content={<ChartTooltipContent />} />
        <Bar dataKey="hours" name="ชั่วโมง" fill="var(--color-hours)" radius={[5, 5, 0, 0]} />
      </BarChart>
    </ChartContainer>
  )
}
