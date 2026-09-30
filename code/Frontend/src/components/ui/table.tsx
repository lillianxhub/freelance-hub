import * as React from "react"
import { cn } from "cn"
import { Button } from "./button"
import { Pagination, PaginationContent, PaginationItem } from "./pagination"
import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

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

function TablePagination({ page, totalPages, total, onPageChange, className }: TablePaginationProps) {
  const safeTotalPages = Math.max(1, totalPages)
  const safePage = Math.min(Math.max(1, page), safeTotalPages)

  return (
    <Pagination className={cn("mt-5", className)}>
      <PaginationContent>
        <PaginationItem>
          <Button variant="outline" type="button" disabled={safePage === 1} onClick={() => onPageChange(safePage - 1)}>
            <ChevronLeftIcon aria-hidden="true" /> ก่อนหน้า
          </Button>
        </PaginationItem>
        <PaginationItem className="px-2 text-sm text-muted-foreground">
          หน้า {safePage} จาก {safeTotalPages}{total === undefined ? '' : ` · ${total} รายการ`}
        </PaginationItem>
        <PaginationItem>
          <Button variant="outline" type="button" disabled={safePage === safeTotalPages} onClick={() => onPageChange(safePage + 1)}>
            ถัดไป <ChevronRightIcon aria-hidden="true" />
          </Button>
        </PaginationItem>
      </PaginationContent>
    </Pagination>
  )
}

function Table({ className, pagination, ...props }: TableProps) {
  return (
    <div
      data-slot="table-container"
      className="relative w-full overflow-x-auto rounded-xl"
    >
      <table data-slot="table" className={cn("w-full rounded-xl border border-border caption-bottom text-sm", className)} {...props} />
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

function TableHead({ className, ...props }: React.ComponentProps<"th">) {
  return (
    <th
      data-slot="table-head"
      className={cn(
        "h-10 !bg-primary-soft px-2 text-left align-middle !text-base !font-semibold whitespace-nowrap !text-text-primary [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    />
  )
}

function TableCell({ className, ...props }: React.ComponentProps<"td">) {
  return (
    <td
      data-slot="table-cell"
      className={cn(
        "p-2 align-middle whitespace-nowrap [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    />
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
