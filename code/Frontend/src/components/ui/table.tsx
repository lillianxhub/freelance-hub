import * as React from "react"
import { cn } from "cn"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "./pagination"
import { getPaginationItems } from '../../utils/pagination'

export interface TablePaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  total?: number
  className?: string
}

type TableProps = React.ComponentProps<"table"> & {
  pagination?: TablePaginationProps
}

function TablePagination({ page, totalPages, onPageChange, className }: TablePaginationProps) {
  const safeTotalPages = Math.max(1, totalPages)
  const safePage = Math.min(Math.max(1, page), safeTotalPages)
  const paginationItems = getPaginationItems(safePage, safeTotalPages)

  return (
    <Pagination className={cn("mt-6", className)}>
      <PaginationContent>
        <PaginationItem>
          <PaginationPrevious
            href={`?page=${safePage - 1}`}
            text="ก่อนหน้า"
            aria-disabled={safePage === 1}
            className={safePage === 1 ? "pointer-events-none opacity-50" : undefined}
            onClick={(event) => {
              event.preventDefault()
              if (safePage > 1) onPageChange(safePage - 1)
            }}
          />
        </PaginationItem>
        {paginationItems.map((item, index) => (
          <PaginationItem key={`${item}-${index}`}>
            {item === "ellipsis" ? (
              <PaginationEllipsis />
            ) : (
              <PaginationLink
                href={`?page=${item}`}
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
            href={`?page=${safePage + 1}`}
            text="ถัดไป"
            aria-disabled={safePage === safeTotalPages}
            className={safePage === safeTotalPages ? "pointer-events-none opacity-50" : undefined}
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

function Table({ className, pagination, ...props }: TableProps) {
  return (
    <div
      data-slot="table-container"
      className="relative min-w-0 w-full rounded-xl"
    >
      <div className="w-full overflow-x-auto rounded-xl">
        <table data-slot="table" className={cn("w-full min-w-[40rem] table-fixed rounded-xl border border-border caption-bottom text-sm", className)} {...props} />
      </div>
      {pagination && <TablePagination {...pagination} />}
    </div>
  )
}

function TableHeader({ className, ...props }: React.ComponentProps<"thead">) {
  return (
    <thead
      data-slot="table-header"
      className={cn("[&_tr]:border-b", className)}
      {...props}
    />
  )
}

function TableBody({ className, ...props }: React.ComponentProps<"tbody">) {
  return (
    <tbody
      data-slot="table-body"
      className={cn("[&_tr:last-child]:border-0", className)}
      {...props}
    />
  )
}

function TableFooter({ className, ...props }: React.ComponentProps<"tfoot">) {
  return (
    <tfoot
      data-slot="table-footer"
      className={cn(
        "border-t bg-muted/50 font-medium [&>tr]:last:border-b-0",
        className
      )}
      {...props}
    />
  )
}

function TableRow({ className, ...props }: React.ComponentProps<"tr">) {
  return (
    <tr
      data-slot="table-row"
      className={cn(
        "border-b transition-colors hover:bg-muted/50 has-aria-expanded:bg-muted/50 data-[state=selected]:bg-muted",
        className
      )}
      {...props}
    />
  )
}

function TableHead({ className, children, ...props }: React.ComponentProps<"th">) {
  return (
    <th
      data-slot="table-head"
      className={cn(
        "h-10 !bg-primary-soft px-2 text-left align-middle !text-sm !font-semibold whitespace-nowrap !text-text-primary [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    >
      {typeof children === "string" || typeof children === "number"
        ? <span className="block min-w-0 truncate" title={String(children)}>{children}</span>
        : children}
    </th>
  )
}

function TableCell({ className, children, ...props }: React.ComponentProps<"td">) {
  return (
    <td
      data-slot="table-cell"
      className={cn(
        "p-2 align-middle whitespace-nowrap [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    >
      {typeof children === "string" || typeof children === "number"
        ? <span className="block min-w-0 truncate" title={String(children)}>{children}</span>
        : children}
    </td>
  )
}

function TableCaption({
  className,
  ...props
}: React.ComponentProps<"caption">) {
  return (
    <caption
      data-slot="table-caption"
      className={cn("mt-4 text-sm text-muted-foreground", className)}
      {...props}
    />
  )
}

export {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableHead,
  TableRow,
  TableCell,
  TableCaption,
  TablePagination,
}
