import { Button } from '../../components/ui/button'
import { useState } from 'react'
import { FiClock, FiSquare } from 'react-icons/fi'
import { useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../../api/apiError'
import { formatTimer } from '../../utils/formatters'
import { useTimer } from '../useTimer'
import { useTimeEntries } from '../useTimeEntries'
import { toast } from 'sonner'

function TopbarTimer() {
  const workspace = useTimeEntries()
  const timer = useTimer(workspace)
  const navigate = useNavigate()
  const [stopping, setStopping] = useState(false)

  if (!timer.runningEntry) return null

  const stopTimer = async () => {
    setStopping(true)
    try {
      await timer.stopTimer()
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
            <strong className="truncate text-xs">{timer.runningProject?.name || 'กำลังบันทึกเวลา'}</strong>
            <small className="truncate text-[10px] text-text-secondary">
              {timer.runningTask?.name || timer.runningEntry.description || 'ไม่ระบุงาน'}
            </small>
          </span>
          <time className="shrink-0 text-xs font-bold tabular-nums">
            {formatTimer(timer.elapsedSeconds)}
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
