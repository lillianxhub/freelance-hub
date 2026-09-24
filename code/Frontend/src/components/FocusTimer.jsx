import { useEffect, useState } from 'react'

const timerStorageKey = 'freelance-hub-focus-timer'

function getInitialTimer() {
  try {
    const saved = JSON.parse(localStorage.getItem(timerStorageKey))
    if (!saved) return { elapsedSeconds: 0, isRunning: false, startedAt: null }

    const timeSinceLastSave = saved.isRunning && saved.startedAt
      ? Math.max(0, Math.floor((Date.now() - saved.startedAt) / 1000))
      : 0

    return {
      elapsedSeconds: (saved.elapsedSeconds || 0) + timeSinceLastSave,
      isRunning: Boolean(saved.isRunning),
      startedAt: saved.isRunning ? Date.now() : null,
    }
  } catch {
    localStorage.removeItem(timerStorageKey)
    return { elapsedSeconds: 0, isRunning: false, startedAt: null }
  }
}

function formatDuration(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
}

function FocusTimer() {
  const [timer, setTimer] = useState(getInitialTimer)

  useEffect(() => {
    localStorage.setItem(timerStorageKey, JSON.stringify(timer))
  }, [timer])

  useEffect(() => {
    if (!timer.isRunning) return undefined

    const intervalId = window.setInterval(() => {
      setTimer((current) => ({ ...current, elapsedSeconds: current.elapsedSeconds + 1 }))
    }, 1000)

    return () => window.clearInterval(intervalId)
  }, [timer.isRunning])

  const toggleTimer = () => {
    setTimer((current) => ({
      ...current,
      isRunning: !current.isRunning,
      startedAt: current.isRunning ? null : Date.now(),
    }))
  }

  const resetTimer = () => {
    setTimer({ elapsedSeconds: 0, isRunning: false, startedAt: null })
  }

  return (
    <div className="panel">
      <h2>Focus timer</h2>
      <p className="panel-subtitle">จับเวลาการทำงานบนเครื่องนี้ระหว่างรอเชื่อม Backend</p>
      <div className={`timer-status${timer.isRunning ? ' running' : ''}`}>
        <span className="timer-status-dot" />
        {timer.isRunning ? 'กำลังจับเวลา' : 'หยุดชั่วคราว'}
      </div>
      <div className="timer-display" aria-live="off">{formatDuration(timer.elapsedSeconds)}</div>
      <p className="timer-meta">เวลาจะถูกเก็บไว้ในเบราว์เซอร์เครื่องนี้</p>
      <div className="timer-actions">
        <button className="btn-primary" type="button" onClick={toggleTimer}>
          {timer.isRunning ? 'หยุดชั่วคราว' : timer.elapsedSeconds > 0 ? 'จับเวลาต่อ' : 'เริ่มจับเวลา'}
        </button>
        <button className="btn-secondary" type="button" onClick={resetTimer} disabled={timer.elapsedSeconds === 0}>รีเซ็ต</button>
      </div>
    </div>
  )
}

export default FocusTimer
