import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '../../components/ui/card'
import { formatDuration } from '../../utils/duration'
import { formatMoney } from '../../utils/number'
import type { TimeSummaryProps } from '../../types/timeTrackerPage'

export default function TimeSummary({
  totalMinutes,
  billableMinutes,
  totalValue,
  count,
}: TimeSummaryProps) {
  return (
    <Card asChild>
      <section className="!p-0">
        <CardHeader className="p-6 pb-4">
          <CardTitle>สรุปตามตัวกรอง</CardTitle>
          <CardDescription>สรุปจากตัวกรองรายการด้านล่าง</CardDescription>
          <CardAction>
            <strong className="text-3xl font-bold leading-tight tabular-nums text-primary">
              {formatDuration(totalMinutes)}
            </strong>
          </CardAction>
        </CardHeader>
        <CardContent className="p-6 pt-0">
          <div className="grid">
            <div className="flex items-center justify-between border-b border-border py-3 text-sm last:border-b-0">
              <span className="text-text-secondary">เวลาที่คิดค่าบริการ</span>
              <strong className="text-base text-text-primary">
                {formatDuration(billableMinutes)}
              </strong>
            </div>
            <div className="flex items-center justify-between border-b border-border py-3 text-sm last:border-b-0">
              <span className="text-text-secondary">สัดส่วนเวลาที่คิดเงินได้</span>
              <strong className="text-base text-text-primary">
                {totalMinutes ? Math.round((billableMinutes / totalMinutes) * 100) : 0}%
              </strong>
            </div>
            <div className="flex items-center justify-between border-b border-border py-3 text-sm last:border-b-0">
              <span className="text-text-secondary">มูลค่าเกิดขึ้น</span>
              <strong className="text-base text-text-primary">{formatMoney(totalValue)}</strong>
            </div>
            <div className="flex items-center justify-between border-b border-border py-3 text-sm last:border-b-0">
              <span className="text-text-secondary">จำนวนรายการ</span>
              <strong className="text-base text-text-primary">{count}</strong>
            </div>
          </div>
        </CardContent>
      </section>
    </Card>
  )
}
