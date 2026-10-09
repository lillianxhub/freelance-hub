export type PaginationItem = number | 'ellipsis'

export function getPaginationItems(currentPage: number, totalPages: number): PaginationItem[] {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, index) => index + 1)

  const pages = new Set([1, totalPages, currentPage - 1, currentPage, currentPage + 1])
  return [...pages]
    .filter((page) => page >= 1 && page <= totalPages)
    .sort((first, second) => first - second)
    .flatMap((page, index, items) =>
      index > 0 && page - items[index - 1] > 1 ? ['ellipsis' as const, page] : [page],
    )
}
