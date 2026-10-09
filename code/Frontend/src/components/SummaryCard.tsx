import type { ReactNode } from 'react'
import { Card } from './ui/card'

type SummaryCardAccent = 'blue' | 'green' | 'violet' | 'orange' | 'red'

interface SummaryCardProps {
  label: string
  icon: ReactNode
  value: ReactNode
  unit?: string
  foot: ReactNode
  accent: SummaryCardAccent
  compact?: boolean
}

const accentClasses: Record<SummaryCardAccent, { card: string; icon: string }> = {
  blue: { card: 'border-t-primary', icon: 'bg-primary-soft text-primary' },
  green: { card: 'border-t-green', icon: 'bg-green-soft text-green' },
  violet: { card: 'border-t-violet', icon: 'bg-violet-soft text-violet' },
  orange: { card: 'border-t-orange', icon: 'bg-orange-soft text-orange' },
  red: { card: 'border-t-destructive', icon: 'bg-red-soft text-destructive' },
}

export default function SummaryCard({
  label,
  icon,
  value,
  unit,
  foot,
  accent,
  compact = false,
}: SummaryCardProps) {
  const classes = accentClasses[accent]

  return (
    <Card
      className={`relative min-w-0 gap-0 rounded-xl border border-border border-t-4 bg-surface p-5 shadow-soft ring-0 ${classes.card}`}
    >
      <div className="flex items-center justify-between gap-3 text-sm text-text-secondary">
        <span>{label}</span>
        <span
          className={`grid size-7 shrink-0 place-items-center rounded-lg text-sm ${classes.icon}`}
          aria-hidden="true"
        >
          {icon}
        </span>
      </div>
      <div
        className={`my-4 break-words text-2xl font-bold leading-none tracking-tight text-text-primary ${compact ? 'text-xl' : ''}`}
      >
        {value}
        {unit && (
          <span className="ml-1 text-sm font-semibold tracking-normal text-text-secondary">
            {unit}
          </span>
        )}
      </div>
      <div className="flex items-center gap-1.5 text-xs text-text-secondary">{foot}</div>
    </Card>
  )
}
