import * as React from 'react'
import { CalendarDays } from 'lucide-react'
import { th } from 'react-day-picker/locale'
import { Button } from './button'
import { Calendar } from './calendar'
import { Popover, PopoverContent, PopoverTrigger } from './popover'
import { cn } from 'cn'

interface DatePickerProps {
  id?: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  disabled?: boolean
  required?: boolean
  onBlur?: React.FocusEventHandler<HTMLButtonElement>
  'aria-label'?: string
  'aria-describedby'?: string
  'aria-invalid'?: boolean
  className?: string
}

function parseDate(value: string): Date | undefined {
  if (!value) return undefined
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) return undefined
  return new Date(year, month - 1, day, 12)
}

function toDateValue(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateValue(value: string): string {
  const date = parseDate(value)
  if (!date) return ''
  return new Intl.DateTimeFormat('th-TH', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function DatePicker({
  id,
  value,
  onChange,
  placeholder = 'วว/ดด/ปปปป',
  disabled = false,
  required = false,
  onBlur,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy,
  'aria-invalid': ariaInvalid,
  className,
}: DatePickerProps) {
  const [open, setOpen] = React.useState(false)
  const selectedDate = parseDate(value)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          disabled={disabled}
          aria-required={required || undefined}
          aria-label={ariaLabel}
          aria-describedby={ariaDescribedBy}
          aria-invalid={ariaInvalid || undefined}
          onBlur={onBlur}
          className={cn(
            'h-[41px] w-full justify-between border-input bg-white px-[11px] py-[9px] text-left font-normal text-text-primary hover:bg-white',
            !value && 'text-muted-foreground',
            className,
          )}
        >
          <span className="truncate">{value ? formatDateValue(value) : placeholder}</span>
          <CalendarDays aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto p-0">
        <Calendar
          mode="single"
          locale={th}
          selected={selectedDate}
          defaultMonth={selectedDate}
          onSelect={(date) => {
            onChange(date ? toDateValue(date) : '')
            setOpen(false)
          }}
          autoFocus
        />
      </PopoverContent>
    </Popover>
  )
}

export { DatePicker }
