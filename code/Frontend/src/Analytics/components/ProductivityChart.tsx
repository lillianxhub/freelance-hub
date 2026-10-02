import { CartesianGrid, Line, LineChart, XAxis, YAxis } from 'recharts'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../components/ui/chart'
import type { ProductivityChartProps } from '../../types/analytics'
import { formatChartDuration } from '../../utils/dashboardChart'

function formatAxisDuration(totalSeconds: number): string {
  if (totalSeconds >= 3600) return `${Number((totalSeconds / 3600).toFixed(1))} ชม.`
  if (totalSeconds >= 60) return `${Number((totalSeconds / 60).toFixed(1))} นาที`
  return `${Math.round(totalSeconds)} วิ`
}

export default function ProductivityChart({ data }: ProductivityChartProps) {
  return (
    <ChartContainer className="h-[255px] w-full aspect-auto" config={{ totalSeconds: { label: 'เวลารวม', color: '#4F6BFF' } }}>
      <AreaChart data={[...data]}>
        <defs>
          <linearGradient id="totalHours" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#4F6BFF" stopOpacity={0.28} />
            <stop offset="95%" stopColor="#4F6BFF" stopOpacity={0.02} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
        <XAxis dataKey="day" tick={{ fontSize: 'var(--font-size-sm)' }} axisLine={false} tickLine={false} />
        <YAxis
          domain={[0, 'auto']}
          width={70}
          tickFormatter={formatAxisDuration}
          tick={{ fontSize: 'var(--font-size-sm)' }}
          axisLine={false}
          tickLine={false}
        />
        <ChartTooltip content={<ChartTooltipContent formatter={(value) => (
          <div className="flex w-full justify-between gap-3">
            <span className="text-muted-foreground">เวลารวม</span>
            <strong className="font-mono text-foreground">{formatChartDuration(Number(value))}</strong>
          </div>
        )} />} />
        <Area type="monotone" dataKey="totalSeconds" name="เวลารวม" stroke="var(--color-totalSeconds)" strokeWidth={2} fill="url(#totalHours)" />
      </AreaChart>
    </ChartContainer>
  )
}
