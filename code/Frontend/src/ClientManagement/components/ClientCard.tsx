import { Card } from '../../components/ui/card'
import { Button } from '../../components/ui/button'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../../components/ui/alert-dialog'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { FiArchive, FiEdit2, FiRotateCcw } from 'react-icons/fi'
import type { ClientCardProps } from '../../types/clientsPage'
import { formatDurationSeconds } from '../../utils/duration'
import { initials } from '../../utils/string'
import ClientStatus from './ClientStatus'

const avatarColors = [
  'bg-primary',
  'bg-brand-secondary',
  'bg-green',
  'bg-orange',
  'bg-red',
] as const

function getAvatarColor(seed: string) {
  const hash = [...seed].reduce((total, character) => total + character.charCodeAt(0), 0)
  return avatarColors[hash % avatarColors.length]
}

export default function ClientCard({
  client,
  projectCount,
  minutes,
  onEdit,
  onArchive,
}: ClientCardProps) {
  const [confirmOpen, setConfirmOpen] = useState(false)
  const displayName = client.name || 'ไม่ระบุชื่อ'
  const companyName = client.company_name || 'ไม่มีชื่อบริษัท'
  const avatarColor = getAvatarColor(client.id || displayName)

  return (
    <Card asChild>
      <article className="relative p-5 transition-[transform,box-shadow] duration-150 hover:-translate-y-0.5 hover:shadow-[0_16px_38px_rgba(30,48,83,0.1)]">
        <Link
          className="absolute inset-0 z-0 rounded-[inherit]"
          to={`/clients/${client.id}`}
          aria-label={`เปิด ${displayName}`}
        />

        <div className="pointer-events-none relative z-10 flex items-start justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <span
              className={`grid size-[42px] shrink-0 place-items-center rounded-xl text-base font-bold text-white ${avatarColor}`}
            >
              {initials(displayName)}
            </span>
            <div className="min-w-0">
              <h2 className="truncate text-base font-semibold text-text-primary">{displayName}</h2>
              <p className="truncate text-sm text-text-secondary">{companyName}</p>
            </div>
          </div>
          <ClientStatus status={client.status} />
        </div>

        <div className="pointer-events-none relative z-10 grid gap-1 text-sm text-text-secondary">
          <p className="truncate">{client.email || 'ยังไม่มีอีเมล'}</p>
          <p className="truncate">{client.phone || 'ยังไม่มีเบอร์โทรศัพท์'}</p>
        </div>

        <div className="pointer-events-none relative z-10 grid grid-cols-2 gap-2 border-y border-border py-3 text-xs text-text-secondary">
          <span className="grid gap-0.5">
            <strong className="text-base font-semibold text-text-primary">{projectCount}</strong>
            โปรเจกต์
          </span>
          <span className="grid gap-0.5">
            <strong className="text-base font-semibold text-text-primary">
              {formatDurationSeconds(minutes * 60)}
            </strong>
            เวลารวม
          </span>
        </div>

        <div className="relative z-10 flex items-center gap-1">
          <Button
            variant="ghost"
            size="sm"
            className="text-muted-foreground hover:bg-muted hover:text-text-primary"
            type="button"
            onClick={() => onEdit(client)}
          >
            <FiEdit2 aria-hidden="true" />
            แก้ไข
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="text-muted-foreground hover:bg-red-soft hover:text-destructive"
            type="button"
            onClick={() => setConfirmOpen(true)}
          >
            {client.status === 'ARCHIVED' ? (
              <FiRotateCcw aria-hidden="true" />
            ) : (
              <FiArchive aria-hidden="true" />
            )}
            {client.status === 'ARCHIVED' ? 'นำกลับ' : 'เก็บถาวร'}
          </Button>
        </div>
        <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>
                {client.status === 'ARCHIVED'
                  ? 'นำลูกค้ากลับมาใช้งานหรือไม่'
                  : 'เก็บลูกค้าเข้าคลังหรือไม่'}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {client.status === 'ARCHIVED'
                  ? 'ลูกค้าจะกลับมาแสดงในรายการที่ใช้งานอยู่'
                  : 'ลูกค้าจะไม่แสดงในรายการที่ใช้งานอยู่ แต่ข้อมูลเดิมจะยังคงอยู่'}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>ยกเลิก</AlertDialogCancel>
              <AlertDialogAction onClick={() => onArchive(client)}>
                {client.status === 'ARCHIVED' ? 'นำกลับมาใช้งาน' : 'เก็บถาวร'}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </article>
    </Card>
  )
}
