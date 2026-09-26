import type { PageHeaderProps } from '../types/ui'

function PageHeader({ eyebrow, title, description, actions }: PageHeaderProps) {
  return (
    <header className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p className="heading-subtitle">{description}</p>}
      </div>
      {actions && <div className="heading-actions">{actions}</div>}
    </header>
  )
}

export default PageHeader
