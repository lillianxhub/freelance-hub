import { formatDuration, formatMoney } from '../../utils/formatters'
import type { TimeSummaryProps } from '../../types/timeTrackerPage'

export default function TimeSummary({ totalMinutes, billableMinutes, totalValue, count }: TimeSummaryProps) {
  return <section className="panel week-summary">
    <div className="panel-heading"><div><h2>Filtered summary</h2><p>สรุปจากตัวกรองรายการด้านล่าง</p></div><strong className="week-hours">{formatDuration(totalMinutes)}</strong></div>
    <div className="summary-list"><div><span>Billable time</span><strong>{formatDuration(billableMinutes)}</strong></div><div><span>Utilization</span><strong>{totalMinutes ? Math.round((billableMinutes / totalMinutes) * 100) : 0}%</strong></div><div><span>มูลค่าเกิดขึ้น</span><strong>{formatMoney(totalValue)}</strong></div><div><span>จำนวนรายการ</span><strong>{count}</strong></div></div>
  </section>
}
