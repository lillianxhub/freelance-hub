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
      <div className="topbar-timer" aria-label="ตัวจับเวลาที่กำลังทำงาน">
        <Button variant="ghost" className="topbar-timer-summary" type="button" onClick={() => navigate('/time-tracker')}>
          <span className="topbar-timer-icon"><FiClock aria-hidden="true" /></span>
          <span className="topbar-timer-copy">
            <strong>{timer.runningProject?.name || 'กำลังบันทึกเวลา'}</strong>
            <small>{timer.runningTask?.name || timer.runningEntry.description || 'ไม่ระบุงาน'}</small>
          </span>
          <time>{formatTimer(timer.elapsedSeconds)}</time>
        </Button>
        <Button variant="ghost" className="topbar-timer-stop" type="button" onClick={() => void stopTimer()} disabled={stopping}>
          <FiSquare aria-hidden="true" />
          <span>{stopping ? 'กำลังหยุด' : 'หยุด'}</span>
        </Button>
      </div>
    </>
  )
}

export default TopbarTimer
