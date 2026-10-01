import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../components/ui/card'
import { Progress } from '../../../components/ui/progress'
import { Button } from '../../../components/ui/button'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../../components/ui/table'
import { useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  FiArrowLeft,
  FiCheckSquare,
  FiClock,
  FiPlus,
} from "react-icons/fi";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../components/ui/dialog";
import PageHeader from "../../../components/PageHeader";
import SummaryCard from "../../../components/SummaryCard";
import StatusBadge from "../../../components/StatusBadge";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useProjects } from "../../useProjects";
import type { Task } from "../../../types/task";
import type { TaskDraft } from "../../../types/projectDetailPage";
import TaskForm from "../../components/TaskForm";
import TaskList from "../../components/TaskList";
import { formatDate, formatDuration } from "../../../lib/formatters";

const emptyTask: TaskDraft = {
  name: "",
  description: "",
  status: "TODO",
  due_date: "",
};

function formatEntryStartTime(value: string) {
  return new Intl.DateTimeFormat("th-TH", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(value));
}

function ProjectDetailPage() {
  const { projectId } = useParams();
  const { data, loading, error, refresh, save, remove } = useProjects();
  const [modalOpen, setModalOpen] = useState(false);
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [formError, setFormError] = useState("");
  const [timeEntryPage, setTimeEntryPage] = useState(1);

  if (loading) return <LoadingState label="LoadingProjects..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const project = data.projects.find((item) => item.id === projectId);
  if (!project) return <ErrorState message="ไม่พบProjectsที่ต้องการ" />;

  const client = data.clients.find((item) => item.id === project.client_id);
  const tasks = data.tasks
    .filter((task) => task.project_id === project.id)
    .sort((a, b) => a.sort_order - b.sort_order);
  const entries = data.time_entries
    .filter((entry) => entry.project_id === project.id)
    .sort((first, second) => Date.parse(second.started_at) - Date.parse(first.started_at));
  const timeEntryPageSize = 5;
  const timeEntryTotalPages = Math.max(1, Math.ceil(entries.length / timeEntryPageSize));
  const safeTimeEntryPage = Math.min(timeEntryPage, timeEntryTotalPages);
  const visibleTimeEntries = entries.slice(
    (safeTimeEntryPage - 1) * timeEntryPageSize,
    safeTimeEntryPage * timeEntryPageSize,
  );
  const totalMinutes = entries.reduce(
    (sum, entry) => sum + (entry.duration_minutes || 0),
    0,
  );
  const completed = project.task_progress?.completed_tasks ?? tasks.filter((task) => task.status === "DONE").length;
  const totalTaskCount = project.task_progress?.total_tasks ?? tasks.length;
  const taskProgress = project.task_progress?.percent ?? (totalTaskCount ? Math.round((completed / totalTaskCount) * 100) : 0);
  const trackedMinutes = project.time_tracking?.tracked_seconds === undefined
    ? totalMinutes
    : project.time_tracking.tracked_seconds / 60;
  const usagePercent = project.time_tracking?.usage_percent ?? null;

  const openTask = (task: Task | TaskDraft = emptyTask) => {
    setTaskForm({ ...emptyTask, ...task });
    setFormError("");
    setModalOpen(true);
  };

  const saveTask = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!taskForm.name.trim()) {
      setFormError("กรุณากรอกชื่องาน");
      return;
    }
    await save("tasks", {
      ...taskForm,
      project_id: project.id,
      name: taskForm.name.trim(),
      sort_order: taskForm.sort_order ?? tasks.length,
    });
    setModalOpen(false);
  };

  const moveTask = async (task: Task, direction: -1 | 1) => {
    const currentIndex = tasks.findIndex((item) => item.id === task.id);
    const nextIndex = currentIndex + direction;
    if (nextIndex < 0 || nextIndex >= tasks.length) return;
    const other = tasks[nextIndex];
    await Promise.all([
      save("tasks", { ...task, sort_order: other.sort_order }),
      save("tasks", { ...other, sort_order: task.sort_order }),
    ]);
  };

  return (
    <div className="mx-auto w-full max-w-screen-2xl">
      <Link className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-primary no-underline hover:text-primary-dark" to="/projects">
        <FiArrowLeft aria-hidden="true" /> กลับไปหน้าโปรเจกต์
      </Link>
      <PageHeader
        title={project.name}
        description={`${project.client_name || client?.company_name || client?.name || "ไม่พบลูกค้า"} · ${project.description || "ไม่มีรายละเอียด"}`}
        actions={
          <>
            <StatusBadge status={project.status} />
            <Button asChild variant="default" className="h-10">
              <Link className="!text-primary-foreground hover:text-primary-foreground" to="/time-tracker">
                <FiClock aria-hidden="true" />
                เริ่มจับเวลา
              </Link>
            </Button>
          </>
        }
      />

      <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <SummaryCard
          label="เวลาที่ใช้"
          icon={<FiClock aria-hidden="true" />}
          value={formatDuration(trackedMinutes)}
          foot={<>จาก {project.budget_hours || "—"} ชั่วโมง</>}
          accent="blue"
          compact
        />
        <SummaryCard
          label="งาน"
          icon={<FiCheckSquare aria-hidden="true" />}
          value={completed}
          unit={`/ ${totalTaskCount}`}
          foot={<>เสร็จแล้ว {taskProgress}%</>}
          accent="violet"
        />
        <SummaryCard
          label="การใช้เวลา"
          icon={<FiClock aria-hidden="true" />}
          value={usagePercent === null ? "—" : Math.round(usagePercent)}
          unit={usagePercent === null ? undefined : "%"}
          foot={usagePercent === null ? "ยังไม่มีข้อมูลงบเวลา" : usagePercent >= 100 ? "เกินงบประมาณ" : "ของชั่วโมงเป้าหมาย"}
          accent={usagePercent !== null && usagePercent >= 100 ? "red" : "orange"}
        />
      </div>

      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1.6fr)_minmax(18rem,0.8fr)]">
        <div className="grid gap-4">
          <Card asChild><section className="!p-0">
            <CardHeader className="flex flex-col items-start justify-between gap-4 p-6 pb-2 sm:flex-row">
              <div className="grid gap-1">
                <CardTitle>งาน</CardTitle>
                <CardDescription>สร้าง ปิดงาน แก้ไข และเรียงลำดับงานในโปรเจกต์</CardDescription>
              </div>
              <Button variant="default"
                className="h-10"
                type="button"
                onClick={() => openTask()}
              >
                <FiPlus aria-hidden="true" /> เพิ่มงาน
              </Button>
            </CardHeader>
            <CardContent className="p-6 pt-0">
              <TaskList
                tasks={tasks}
                onToggle={(task) =>
                  save("tasks", {
                    ...task,
                    status: task.status === "DONE" ? "TODO" : "DONE",
                  })
                }
                onMove={moveTask}
                onEdit={openTask}
                onDelete={(task) => remove("tasks", task.id)}
              />
            </CardContent>
          </section></Card>

          <Card asChild><section className="!p-0">
            <CardHeader className="p-6 pb-2">
              <div className="grid gap-1">
                <CardTitle>รายการเวลาล่าสุด</CardTitle>
                <CardDescription>เวลาล่าสุดที่บันทึกในโปรเจกต์</CardDescription>
              </div>
            </CardHeader>
            <CardContent className="p-6 pt-0">
              <Table
                pagination={{
                  page: safeTimeEntryPage,
                  totalPages: timeEntryTotalPages,
                  total: entries.length,
                  onPageChange: setTimeEntryPage,
                }}
              >
                <TableHeader>
                  <TableRow>
                    <TableHead className="min-w-56">งาน / รายละเอียด</TableHead>
                    <TableHead className="w-40">เริ่มจับเวลา</TableHead>
                    <TableHead className="w-32 text-right">ระยะเวลา</TableHead>
                    {/* ส่วนคิดค่าบริการและมูลค่าถูกซ่อนไว้ชั่วคราว */}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {visibleTimeEntries.map((entry) => (
                    <TableRow key={entry.id}>
                      <TableCell className="min-w-0 whitespace-normal">
                        <div className="grid gap-1">
                          <strong className="truncate text-sm font-semibold text-text-primary">
                            {tasks.find((task) => task.id === entry.task_id)?.name || entry.task_name || "ไม่ระบุงาน"}
                          </strong>
                          <span className="truncate text-xs text-text-secondary">
                            {entry.description?.trim() || "ไม่มีรายละเอียด"}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-text-secondary">
                        <div className="grid gap-0.5">
                          <span>{formatDate(entry.started_at)}</span>
                          <span className="text-xs text-muted-foreground">{formatEntryStartTime(entry.started_at)} น.</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-semibold tabular-nums text-text-primary">
                        {formatDuration(entry.duration_minutes)}
                      </TableCell>
                      {/* <td>{entry.billable ? "ใช่" : "ไม่"}</td>
                      <td>{formatMoney(calculateTimeValue(entry), entry.currency)}</td> */}
                    </TableRow>
                  ))}
                  {entries.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={3}>ยังไม่มีรายการเวลา</TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </section></Card>
        </div>

        <Card asChild><aside className="!p-0">
          <CardHeader className="p-6 pb-2">
            <CardTitle>รายละเอียดโปรเจกต์</CardTitle>
            <CardDescription>ขอบเขตและกำหนดการ</CardDescription>
          </CardHeader>
          <CardContent className="p-6 pt-0">
            <div className="grid gap-4">
            <div className="grid gap-1.5 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary [&>strong]:text-sm [&>strong]:leading-relaxed">
              <span>ลูกค้า</span>
              <Link className="font-semibold text-primary hover:underline" to={`/clients/${client?.id}`}>
                <strong>{client?.company_name || client?.name || project.client_name || "ไม่พบลูกค้า"}</strong>
              </Link>
            </div>
            <div className="grid gap-1.5 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary [&>strong]:text-sm [&>strong]:leading-relaxed">
              <span>สถานะ</span>
              <StatusBadge status={project.status} />
            </div>
            <div className="grid gap-1.5 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary [&>strong]:text-sm [&>strong]:leading-relaxed">
              <span>วันที่เริ่ม</span>
              <strong>{formatDate(project.start_date)}</strong>
            </div>
            <div className="grid gap-1.5 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary [&>strong]:text-sm [&>strong]:leading-relaxed">
              <span>วันที่สิ้นสุด</span>
              <strong>{formatDate(project.end_date)}</strong>
            </div>
            <div className="grid gap-1.5 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary [&>strong]:text-sm [&>strong]:leading-relaxed">
              <span>ชั่วโมงเป้าหมาย</span>
              <strong>{project.budget_hours === null ? "—" : `${project.budget_hours} ชั่วโมง`}</strong>
            </div>
            <div className="grid gap-2 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary">
              <span>เวลาที่บันทึก</span>
              <strong>{formatDuration(trackedMinutes)}</strong>
            </div>
            <div className="grid gap-2 [&>span:first-child]:text-xs [&>span:first-child]:text-text-secondary">
              <span>ความคืบหน้างาน</span>
              <div className="flex items-center justify-between gap-3 text-sm">
              <span>
                  {completed}/{totalTaskCount}
                </span>
                <strong>{taskProgress}%</strong>
              </div>
              <Progress className="h-2 bg-border" value={taskProgress} indicatorColor={project.color} />
            </div>
            </div>
          </CardContent>
        </aside></Card>
      </div>

      <Dialog open={modalOpen} onOpenChange={(open) => { if (!open) setModalOpen(false) }}>
        <DialogContent className="!max-w-2xl max-h-[calc(100dvh-2rem)] overflow-y-auto">
          <DialogHeader><DialogTitle>{taskForm.id ? "แก้ไข Task" : "เพิ่ม Task"}</DialogTitle></DialogHeader>
          <TaskForm
            value={taskForm}
            error={formError}
            onChange={setTaskForm}
            onSubmit={saveTask}
            onCancel={() => setModalOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default ProjectDetailPage;
