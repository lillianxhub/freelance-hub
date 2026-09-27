import { calculateTimeValue, formatDate, formatDuration, formatMoney } from '../../utils/formatters'
import type { RecentActivityProps } from '../../types/analytics'

export default function RecentActivity({ entries, projects }: RecentActivityProps) {
  return <div className="list-stack">{entries.map((entry) => {
    const project = projects.find((item) => item.id === entry.project_id)
    return <div className="list-item" key={entry.id}><span className="color-dot" style={{ '--dot-color': project?.color }} /><span><strong>{entry.description || project?.name}</strong><small>{project?.name} · {formatDate(entry.started_at)}</small></span><span>{formatDuration(entry.duration_minutes)}</span><strong>{entry.billable ? formatMoney(calculateTimeValue(entry), entry.currency) : 'ไม่billable'}</strong></div>
  })}</div>
}
