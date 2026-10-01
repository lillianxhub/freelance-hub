import type { PageHeaderProps } from '../types/ui'

function PageHeader({ eyebrow, title, description, actions }: PageHeaderProps) {
  return (
    <header className="mb-[25px] flex items-end justify-between gap-6 max-[820px]:items-start max-[820px]:flex-col">
      <div>
        {eyebrow && <p className="mb-1.5 text-xs font-bold uppercase tracking-[0.12em] text-primary">{eyebrow}</p>}
        <h1 className="text-[clamp(28px,3vw,38px)] font-bold leading-[1.15] tracking-[-0.035em] text-text-primary">{title}</h1>
        {description && <p className="mt-2 text-sm leading-relaxed text-text-secondary">{description}</p>}
      </div>
      {actions && <div className="flex items-center gap-2 max-[820px]:w-full max-[820px]:flex-wrap">{actions}</div>}
    </header>
  )
}

export default PageHeader
