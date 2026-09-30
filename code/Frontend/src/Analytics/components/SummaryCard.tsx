import type { SummaryCardProps } from '../../types/analytics'
import { Card } from '../../components/ui/card'

export default function SummaryCard({ label, icon, value, unit, foot, accent, compact = false }: SummaryCardProps) {
  return <Card asChild><article className={`metric-card accent-${accent}`}>
    <div className="metric-top"><span>{label}</span><span className="metric-icon">{icon}</span></div>
    <div className={`metric-value${compact ? ' metric-compact' : ''}`}>{value}{unit && <span className="metric-unit">{unit}</span>}</div>
    <div className="metric-foot">{foot}</div>
  </article></Card>
}
