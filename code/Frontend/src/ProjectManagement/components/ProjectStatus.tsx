import StatusBadge from '../../components/StatusBadge'
import type { ProjectStatusProps } from '../../types/projectsPage'

export default function ProjectStatus({ status }: ProjectStatusProps) {
  return <StatusBadge status={status} />
}
