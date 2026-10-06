import { Button } from '../../components/ui/button'
import { useEffect, useState } from 'react'
import { FiClock, FiSquare } from 'react-icons/fi'
import { useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../../api/apiError'
import { formatTimer } from '../../utils/duration'
import { stopTimer as stopTimerRequest } from '../../services/timeTracking'
import { useCurrentTimer } from '../useCurrentTimer'
import { toast } from 'sonner'

function TopbarTimer() {
  const { currentTimer, refreshCurrentTimer } = useCurrentTimer()
  const navigate = useNavigate()
  const [stopping, setStopping] = useState(false)
  const [now, setNow] = useState(() => Date.now())
  const entry = currentTimer?.running ? currentTimer.timeEntry : null
  const startedAt = entry?.startedAt

  useEffect(() => {
    if (!startedAt) return undefined
    const interval = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(interval)
  }, [startedAt])

  if (!entry) return null

  const stopTimer = async () => {
    setStopping(true)
    try {
      await stopTimerRequest()
      await refreshCurrentTimer()
    } catch (stopError: unknown) {
      toast.error(getErrorMessage(stopError, 'ไม่สามารถหยุดเวลาได้'))
    } finally {
      setStopping(false)
    }
  }

  return (
    <>
      <div className="flex min-w-0 items-stretch overflow-hidden rounded-[10px] border border-[#d9e1ff] bg-primary-soft text-primary-dark" aria-label="ตัวจับเวลาที่กำลังทำงาน">
        <Button variant="ghost" className="h-auto min-w-0 gap-2 bg-transparent px-[9px] py-[5px] text-left text-primary-dark hover:bg-primary/10 max-[560px]:gap-[5px] max-[560px]:px-[7px]" type="button" onClick={() => navigate('/time-tracker')}>
          <span className="grid size-6 shrink-0 place-items-center rounded-full bg-white">
            <FiClock className="size-[14px]" aria-hidden="true" />
          </span>
          <span className="flex min-w-0 max-w-[140px] flex-col max-[820px]:hidden">
            <strong className="truncate text-xs">{entry.project.name || 'กำลังบันทึกเวลา'}</strong>
            <small className="truncate text-[10px] text-text-secondary">
              {entry.task?.title || entry.description || 'ไม่ระบุงาน'}
            </small>
          </span>
          <time className="shrink-0 text-xs font-bold tabular-nums">
            {formatTimer(Math.max(0, Math.floor((now - Date.parse(entry.startedAt)) / 1000)))}
          </time>
        </Button>
        <button
          className="ml-1 grid size-8 shrink-0 cursor-pointer place-items-center self-center border-0 bg-transparent p-0 text-destructive hover:text-destructive/80 focus-visible:rounded-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-destructive/30 disabled:cursor-wait disabled:opacity-65"
          type="button" onClick={() => void stopTimer()}
          disabled={stopping}
          aria-label="หยุดและบันทึกเวลา" title="หยุดและบันทึกเวลา">
          <FiSquare aria-hidden="true" />
        </button>
      </div>
    </>
  )
}

export default TopbarTimer
