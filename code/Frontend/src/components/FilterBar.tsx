import type { ChangeEventHandler, PropsWithChildren } from 'react'
import { FiSearch } from 'react-icons/fi'
import { cn } from 'cn'
import { Input } from './ui/input'

interface FilterBarProps extends PropsWithChildren {
  placeholder?: string
  value: string
  onChange: ChangeEventHandler<HTMLInputElement>
  searchAriaLabel?: string
  className?: string
}

export default function FilterBar({
  placeholder = 'ค้นหา...',
  value,
  onChange,
  searchAriaLabel = 'ค้นหา',
  className,
  children,
}: FilterBarProps) {
  return (
    <div className={cn('mb-[18px] flex items-center gap-2.5 rounded-[13px] border border-border bg-surface p-[11px] max-[680px]:flex-wrap', className)}>
      <div className="relative min-w-[220px] flex-1 max-[680px]:basis-full">
        <FiSearch className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-subtle" aria-hidden="true" />
        <Input
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          aria-label={searchAriaLabel}
          className="h-[38px] border-0 bg-transparent pl-[34px] shadow-none focus-visible:border-0 focus-visible:ring-0"
        />
      </div>
      <div className="flex items-center gap-2.5 max-[680px]:w-full max-[680px]:flex-wrap [&>[data-slot=select-trigger]]:min-w-[145px] max-[680px]:[&>[data-slot=select-trigger]]:flex-1">
        {children}
      </div>
    </div>
  )
}
