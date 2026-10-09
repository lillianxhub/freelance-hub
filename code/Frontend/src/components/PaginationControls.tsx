import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from './ui/pagination'
import { getPaginationItems } from '../utils/pagination'

interface PaginationControlsProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  queryParam?: string
  className?: string
}

export default function PaginationControls({
  page,
  totalPages,
  onPageChange,
  queryParam = 'page',
  className,
}: PaginationControlsProps) {
  const safeTotalPages = Math.max(1, totalPages)
  const safePage = Math.min(Math.max(1, page), safeTotalPages)
  const items = getPaginationItems(safePage, safeTotalPages)
  const href = (nextPage: number) => `?${queryParam}=${nextPage}`

  return (
    <Pagination className={className}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            href={href(Math.max(1, safePage - 1))}
            text="ก่อนหน้า"
            aria-disabled={safePage === 1}
            className={safePage === 1 ? 'pointer-events-none opacity-50' : undefined}
            onClick={(event) => {
              event.preventDefault()
              if (safePage > 1) onPageChange(safePage - 1)
            }}
          />
        </PaginationItem>
        {items.map((item, index) => (
          <PaginationItem key={`${item}-${index}`}>
            {item === 'ellipsis' ? (
              <PaginationEllipsis />
            ) : (
              <PaginationLink
                href={href(item)}
                isActive={item === safePage}
                onClick={(event) => {
                  event.preventDefault()
                  onPageChange(item)
                }}
              >
                {item}
              </PaginationLink>
            )}
          </PaginationItem>
        ))}
        <PaginationItem>
          <PaginationNext
            href={href(Math.min(safeTotalPages, safePage + 1))}
            text="ถัดไป"
            aria-disabled={safePage === safeTotalPages}
            className={safePage === safeTotalPages ? 'pointer-events-none opacity-50' : undefined}
            onClick={(event) => {
              event.preventDefault()
              if (safePage < safeTotalPages) onPageChange(safePage + 1)
            }}
          />
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  )
}
