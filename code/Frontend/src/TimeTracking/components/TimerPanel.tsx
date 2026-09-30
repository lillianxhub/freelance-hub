import { Card } from '../../components/ui/card'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Label } from '../../components/ui/label'
import { NativeSelect } from '../../components/ui/native-select'
import { Input } from '../../components/ui/input'
import { formatTimer } from "../../utils/formatters";
import { FiPlay, FiSquare } from "react-icons/fi";
import { useTimer } from "../useTimer";
import type { WorkspaceContextValue } from "../../types/workspaceContext";
import { getErrorMessage } from '../../api/apiError'
import { toast } from 'sonner'

interface TimerPanelProps {
  workspace: WorkspaceContextValue;
}

export default function TimerPanel({ workspace }: TimerPanelProps) {
  const timer = useTimer(workspace, { loadTaskOptions: true });
  const runTimerAction = async (action: () => Promise<void>) => {
    try {
      await action()
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, 'ไม่สามารถดำเนินการกับตัวจับเวลาได้'))
    }
  }

  return (
    <>
    <Card asChild><section className="panel big-timer-card">
      <Badge variant="secondary" className={`timer-pill${timer.runningEntry ? " live" : ""}`}>
        <i />
        {timer.runningEntry ? "Timer กำลังทำTask" : "พร้อมเริ่มTask"}
      </Badge>
      <div className="big-timer">{formatTimer(timer.elapsedSeconds)}</div>
      {timer.runningEntry ? (
        <>
          <div className="running-project">
            <span
              className="color-dot"
              style={{ "--dot-color": timer.runningProject?.color }}
            />
            <span>
              <strong>{timer.runningProject?.name}</strong>
              <small>โปรเจกต์ที่กำลังจับเวลา</small>
            </span>
          </div>
          <div className="running-timer-details">
            <span><small>งาน</small><strong>{timer.runningTask?.name || "ไม่ระบุงาน"}</strong></span>
            <span><small>รายละเอียด</small><strong>{timer.runningEntry.description || "ไม่มีรายละเอียด"}</strong></span>
          </div>
          <div className="timer-button-row">
            <Button variant="destructive"
              className="button button-danger wide"
              type="button"
              onClick={() => void runTimerAction(timer.stopTimer)}
            >
              <FiSquare aria-hidden="true" /> หยุดและบันทึก
            </Button>
            <Button variant="outline"
              className="button button-secondary"
              type="button"
              onClick={() => void runTimerAction(timer.cancelTimer)}
            >
              ยกเลิก
            </Button>
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
            <div className="form-field">
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
          </div>
          {timer.optionsError && (
            <p className="form-message error">{timer.optionsError}</p>
          )}
          <div className="form-field">
            <Label htmlFor="timer-description">คำอธิบาย</Label>
            <Input
              id="timer-description"
              value={timer.description}
              onChange={(event) => timer.setDescription(event.target.value)}
              placeholder="กำลังทำอะไรอยู่?"
            />
          </div>
          {/* <label className="check-field timer-billable">
            <input
              type="checkbox"
              checked={timer.billable}
              onChange={(event) => timer.setBillable(event.target.checked)}
            />
            <span>เวลาที่คิดค่าบริการ</span>
          </label> */}
          <Button variant="default"
            className="button button-primary wide"
            type="button"
            disabled={!timer.timerProjectId}
            onClick={() => void runTimerAction(timer.startTimer)}
          >
            <FiPlay aria-hidden="true" /> เริ่มจับเวลา
          </Button>
        </>
      )}
    </section></Card>
    </>
  );
}
