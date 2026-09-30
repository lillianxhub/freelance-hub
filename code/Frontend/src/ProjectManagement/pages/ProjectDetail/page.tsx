import { Card } from '../../../components/ui/card'
import { Progress } from '../../../components/ui/progress'
import { Button } from '../../../components/ui/button'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../../components/ui/table'
import { useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  FiArrowLeft,
  FiArrowRight,
  FiCheckSquare,
  FiClock,
  FiPlus,
} from "react-icons/fi";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../components/ui/dialog";
import PageHeader from "../../../components/PageHeader";
import StatusBadge from "../../../components/StatusBadge";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useProjects } from "../../useProjects";
import type { Task } from "../../../types/task";
import type { TaskDraft } from "../../../types/projectDetailPage";
import TaskForm from "../../components/TaskForm";
import TaskList from "../../components/TaskList";
import { formatDate, formatDuration } from "../../../utils/formatters";

const emptyTask: TaskDraft = {
  name: "",
  description: "",
  status: "TODO",
  due_date: "",
};

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
  const timeEntryPageSize = 6;
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
  const completed = tasks.filter((task) => task.status === "DONE").length;
  const taskProgress = tasks.length
    ? Math.round((completed / tasks.length) * 100)
    : 0;

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
    <div className="page-view">
      <Link className="back-link" to="/projects">
        <FiArrowLeft aria-hidden="true" /> กลับไปหน้าโปรเจกต์
      </Link>
      <PageHeader
        eyebrow="พื้นที่ทำงาน / โปรเจกต์"
        title={project.name}
        description={`${project.client_name || client?.company_name || client?.name || "ไม่พบลูกค้า"} · ${project.description || "ไม่มีรายละเอียด"}`}
        actions={
          <>
            <StatusBadge status={project.status} />
            <Button asChild variant="default"><Link className="button button-primary" to="/time-tracker">
              <FiClock aria-hidden="true" /> เริ่มจับเวลา
            </Link></Button>
          </>
        }
      />

      <div className="summary-grid project-detail-summary-grid">
        <Card asChild><article className="metric-card accent-blue">
          <div className="metric-top">
            <span>เวลาที่ใช้</span>
            <span className="metric-icon">
              <FiClock aria-hidden="true" />
            </span>
          </div>
          <div className="metric-value metric-compact">
            {formatDuration(totalMinutes)}
          </div>
          <div className="metric-foot">
            จากงบ {project.budget_hours || "—"} ชั่วโมง
          </div>
        </article></Card>
        {/* <article className="metric-card accent-green">
          <div className="metric-top">
            <span>มูลค่าเกิดขึ้น</span>
            <span className="metric-icon">฿</span>
          </div>
          <div className="metric-value metric-compact">
            {formatMoney(
              project.billing_type === "FIXED_PRICE"
                ? project.fixed_price
                : billableValue,
              project.currency,
            )}
          </div>
          <div className="metric-foot">
            {project.billing_type === "HOURLY"
              ? "คำนวณจาก เวลาที่billable"
              : "มูลค่า fixed price"}
          </div>
        </article> */}
        <Card asChild><article className="metric-card accent-violet">
          <div className="metric-top">
            <span>งาน</span>
            <span className="metric-icon">
              <FiCheckSquare aria-hidden="true" />
            </span>
          </div>
          <div className="metric-value">
            {completed}
            <span className="metric-unit">/ {tasks.length}</span>
          </div>
          <div className="metric-foot">เสร็จแล้ว {taskProgress}%</div>
        </article></Card>
        {/* <article
          className={`metric-card ${budgetPercent >= 100 ? "accent-red" : "accent-orange"}`}
        >
          <div className="metric-top">
            <span>การใช้งบ</span>
            <span className="metric-icon">◔</span>
          </div>
          <div className="metric-value">
            {budgetPercent}
            <span className="metric-unit">%</span>
          </div>
          <div className="metric-foot">
            {budgetPercent >= 100
              ? "เกินงบประมาณ"
              : budgetPercent >= 80
                ? "ใกล้ถึงงบประมาณ"
                : "ยังอยู่ในแผน"}
          </div>
        </article> */}
      </div>

      <div className="detail-grid">
        <div className="section-stack">
          <Card asChild><section className="panel">
            <div className="panel-heading">
              <div>
                <h2>งาน</h2>
                <p>สร้าง ปิดงาน แก้ไข และเรียงลำดับงานในโปรเจกต์</p>
              </div>
              <Button variant="default"
                className="button button-primary"
                type="button"
                onClick={() => openTask()}
              >
                <FiPlus aria-hidden="true" /> เพิ่มงาน
              </Button>
            </div>
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
          </section></Card>

          <Card asChild><section className="panel">
            <div className="panel-heading">
              <div>
                <h2>รายการเวลาล่าสุด</h2>
                <p>เวลาล่าสุดที่บันทึกในโปรเจกต์</p>
              </div>
              <Button asChild variant="ghost"><Link className="text-button" to="/time-tracker">
                ดูทั้งหมด <FiArrowRight aria-hidden="true" />
              </Link></Button>
            </div>
            <div className="table-wrap">
              <Table
                className="data-table"
                pagination={{
                  page: safeTimeEntryPage,
                  totalPages: timeEntryTotalPages,
                  total: entries.length,
                  onPageChange: setTimeEntryPage,
                }}
              >
                <TableHeader>
                  <TableRow>
                    <TableHead>รายละเอียด</TableHead>
                    <TableHead>วันที่</TableHead>
                    <TableHead>ระยะเวลา</TableHead>
                    {/* ส่วนคิดค่าบริการและมูลค่าถูกซ่อนไว้ชั่วคราว */}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {visibleTimeEntries.map((entry) => (
                    <TableRow key={entry.id}>
                      <TableCell>
                        <strong>
                          {entry.description || "ไม่มีรายละเอียด"}
                        </strong>
                      </TableCell>
                      <TableCell>{formatDate(entry.started_at)}</TableCell>
                      <TableCell>{formatDuration(entry.duration_minutes)}</TableCell>
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
            </div>
          </section></Card>
        </div>

        <Card asChild><aside className="panel">
          <div className="panel-heading">
            <div>
              <h2>รายละเอียดโปรเจกต์</h2>
              <p>ขอบเขตและกำหนดการ</p>
            </div>
          </div>
          <div className="detail-list">
            <div className="detail-item">
              <span>ลูกค้า</span>
              <Link to={`/clients/${client?.id}`}>
                <strong>{client?.company_name || client?.name}</strong>
              </Link>
            </div>
            {/* <div className="detail-item">
              <span>รูปแบบราคา</span>
              <strong>
                {project.billing_type === "HOURLY" ? "รายชั่วโมง" : "เหมาจ่าย"}
              </strong>
            </div>
            <div className="detail-item">
              <span>อัตรา/มูลค่า</span>
              <strong>
                {formatMoney(
                  project.billing_type === "HOURLY"
                    ? project.hourly_rate
                    : project.fixed_price,
                  project.currency,
                )}
                {project.billing_type === "HOURLY" && "/ชม."}
              </strong>
            </div> */}
            <div className="detail-item">
              <span>วันที่เริ่ม</span>
              <strong>{formatDate(project.start_date)}</strong>
            </div>
            <div className="detail-item">
              <span>วันที่สิ้นสุด</span>
              <strong>{formatDate(project.end_date)}</strong>
            </div>
            {/* {effectiveRate !== null && (
              <div className="detail-item">
                <span>อัตราต่อชั่วโมงโดยเฉลี่ย</span>
                <strong>
                  {formatMoney(effectiveRate, project.currency)}/ชม.
                </strong>
              </div>
            )} */}
            <div className="detail-item">
              <span>ความคืบหน้างาน</span>
              <div className="progress-label">
                <span>
                  {completed}/{tasks.length}
                </span>
                <strong>{taskProgress}%</strong>
              </div>
              <Progress className="progress-track" value={taskProgress} indicatorColor={project.color} />
            </div>
          </div>
        </aside></Card>
      </div>

      <Dialog open={modalOpen} onOpenChange={(open) => { if (!open) setModalOpen(false) }}>
        <DialogContent className="workspace-dialog">
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
