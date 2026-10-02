import { Card } from '../../components/ui/card'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Label } from '../../components/ui/label'
import { NativeSelect } from '../../components/ui/native-select'
import { Input } from '../../components/ui/input'
import { formatTimer } from "../../utils/formatters";
import { FiPlay, FiSquare } from "react-icons/fi";
import { useTimer } from "../useTimer";
import type { TimerWorkspace } from "../../types/timerWorkspace";
import { getErrorMessage } from '../../api/apiError'
import { toast } from 'sonner'

interface TimerPanelProps {
  workspace: TimerWorkspace;
  onProjectOptionsOpen?: () => void;
}

export default function TimerPanel({ workspace, onProjectOptionsOpen }: TimerPanelProps) {
  const timer = useTimer(workspace, { loadTaskOptions: true });
  const runTimerAction = async (action: () => Promise<void>) => {
    try {
      await action()
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, 'ไม่สามารถดำเนินการกับตัวจับเวลาได้'))
    } finally {
      setPendingAction(null)
    }
  }

  const pendingActionLabel = pendingAction === 'starting'
    ? 'กำลังเริ่มจับเวลา...'
    : pendingAction === 'stopping'
      ? 'กำลังหยุดเวลา...'
      : pendingAction === 'cancelling'
        ? 'กำลังยกเลิก...'
        : ''

  return (
    <Card asChild>
      <section className="min-h-[250px] items-stretch gap-6 px-5 py-5 sm:px-6 sm:py-6">
        <div className="flex w-full items-start justify-between gap-4">
          <div className="text-[clamp(42px,10vw,112px)] leading-[0.9] font-bold tracking-[0.01em] text-text-primary tabular-nums">
            {timerLeading}:<span className="text-subtle">{timerSeconds}</span>
          </div>
          <Badge
            variant="secondary"
            className={`h-auto shrink-0 gap-2 rounded-full border-0 px-3 py-1.5 text-xs font-semibold ${timer.runningEntry ? 'bg-green-soft text-green' : 'bg-surface-soft text-text-secondary'}`}
          >
            <span className={`size-2 rounded-full ${timer.runningEntry ? 'bg-green shadow-[0_0_0_3px_rgba(27,156,104,0.12)]' : 'bg-subtle'}`} />
            {timer.runningEntry ? 'กำลังจับเวลา' : 'พร้อมเริ่มทำงาน'}
          </Badge>
        </div>

        {pendingActionLabel && (
          <div className="flex items-center gap-2 text-sm text-text-secondary" role="status" aria-live="polite">
            <FiLoader className="size-4 animate-spin" aria-hidden="true" />
            <span>{pendingActionLabel}</span>
          </div>
        </>
      ) : (
        <>
          <div className="timer-selects">
            <div className="form-field">
              <Label htmlFor="timer-project">โปรเจกต์</Label>
              <NativeSelect
                id="timer-project"
                value={timer.timerProjectId}
                onOpenChange={(open) => { if (open) onProjectOptionsOpen?.() }}
                onChange={(event) => {
                  timer.setSelectedProject(event.target.value);
                  timer.setSelectedTask("");
                }}
              >
                {timer.activeProjects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </NativeSelect>
            </div>

            <div className="flex w-full items-center gap-2 lg:min-w-[205px]">
              <Button
                variant="destructive"
                className="h-[41px] flex-1 gap-2 whitespace-nowrap"
                type="button"
                disabled={pendingAction !== null}
                onClick={() => void runTimerAction(timer.stopTimer, 'stopping')}
              >
                <FiSquare aria-hidden="true" /> หยุดและบันทึก
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="size-10 shrink-0 text-destructive hover:bg-red-soft hover:text-destructive"
                type="button"
                disabled={pendingAction !== null}
                onClick={() => void runTimerAction(timer.cancelTimer, 'cancelling')}
                aria-label="ยกเลิกและลบการจับเวลา"
                title="ยกเลิกและลบการจับเวลา"
              >
                <FiTrash2 aria-hidden="true" />
              </Button>
            </div>
          </div>
        ) : (
          <div className="w-full space-y-4">
            <div className="grid grid-cols-1 items-end gap-3 sm:grid-cols-2 lg:grid-cols-[minmax(170px,1fr)_minmax(170px,1fr)_minmax(240px,1.6fr)_auto]">
              <div className="grid gap-2">
                <Label htmlFor="timer-project">โปรเจกต์</Label>
                <NativeSelect
                  id="timer-project"
                  value={timer.timerProjectId}
                  onChange={(event) => {
                    timer.setSelectedProject(event.target.value)
                    timer.setSelectedTask('')
                  }}
                >
                  {timer.activeProjects.map((project) => (
                    <option key={project.id} value={project.id}>
                      {project.name}
                    </option>
                  ))}
                </NativeSelect>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="timer-task">งาน</Label>
                <NativeSelect
                  id="timer-task"
                  value={timer.selectedTask}
                  onChange={(event) => timer.setSelectedTask(event.target.value)}
                >
                  <option value="">ไม่ระบุงาน</option>
                  {timer.selectedTasks.map((task) => (
                    <option key={task.id} value={task.id}>
                      {task.name}
                    </option>
                  ))}
                </NativeSelect>
              </div>

            <div className="grid gap-2">
              <Label htmlFor="timer-description">คำอธิบาย</Label>
              <Input
                id="timer-description"
                value={timer.description}
                onChange={(event) => timer.setDescription(event.target.value)}
                placeholder="กำลังทำอะไรอยู่?"
              />
            </div>

            <Button
              variant="default"
              className="h-[41px] w-full gap-2 whitespace-nowrap lg:min-w-[165px]"
              type="button"
              disabled={!timer.timerProjectId || pendingAction !== null}
              onClick={() => void runTimerAction(timer.startTimer, 'starting')}
            >
              <FiPlay aria-hidden="true" /> เริ่มจับเวลา
            </Button>
            </div>

            {timer.optionsError && (
              <p className="text-sm text-destructive">{timer.optionsError}</p>
            )}
          </div>
        )}
      </section>
    </Card>
  )
}
