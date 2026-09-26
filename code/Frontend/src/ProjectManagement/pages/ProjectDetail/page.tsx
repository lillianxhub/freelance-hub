import { useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import Modal from "../../../components/Modal";
import PageHeader from "../../../components/PageHeader";
import StatusBadge from "../../../components/StatusBadge";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useWorkspace } from "../../../Workspace/useWorkspace";
import type { Task } from "../../../types/task";
import type { TaskDraft } from "../../../types/projectDetailPage";
import TaskForm from "../../components/TaskForm";
import TaskList from "../../components/TaskList";
import {
  calculateTimeValue,
  formatDate,
  formatDuration,
  formatMoney,
} from "../../../utils/formatters";

const emptyTask: TaskDraft = {
  name: "",
  description: "",
  status: "TODO",
  due_date: "",
};

function ProjectDetailPage() {
  const { projectId } = useParams();
  const { data, loading, error, refresh, save, remove } = useWorkspace();
  const [modalOpen, setModalOpen] = useState(false);
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [formError, setFormError] = useState("");

  if (loading) return <LoadingState label="LoadingProjects..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const project = data.projects.find((item) => item.id === projectId);
  if (!project) return <ErrorState message="ไม่พบProjectsที่ต้องการ" />;

  const client = data.clients.find((item) => item.id === project.client_id);
  const tasks = data.tasks
    .filter((task) => task.project_id === project.id)
    .sort((a, b) => a.sort_order - b.sort_order);
  const entries = data.time_entries.filter(
    (entry) => entry.project_id === project.id,
  );
  const totalMinutes = entries.reduce(
    (sum, entry) => sum + (entry.duration_minutes || 0),
    0,
  );
  const billableValue = entries.reduce(
    (sum, entry) => sum + calculateTimeValue(entry),
    0,
  );
  const completed = tasks.filter((task) => task.status === "DONE").length;
  const taskProgress = tasks.length
    ? Math.round((completed / tasks.length) * 100)
    : 0;
  const budgetPercent = project.budget_hours
    ? Math.round((totalMinutes / 60 / project.budget_hours) * 100)
    : 0;
  const effectiveRate =
    project.billing_type === "FIXED_PRICE" && totalMinutes
      ? (project.fixed_price ?? 0) / (totalMinutes / 60)
      : null;

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
        ← กลับไปหน้าโปรเจกต์
      </Link>
      <PageHeader
        eyebrow="พื้นที่ทำงาน / โปรเจกต์"
        title={project.name}
        description={`${client?.company_name || client?.name} · ${project.description || "ไม่มีรายละเอียด"}`}
        actions={
          <>
            <StatusBadge status={project.status} />
            <Link className="button button-primary" to="/time-tracker">
              ◷ เริ่มจับเวลา
            </Link>
          </>
        }
      />

      <div className="summary-grid">
        <article className="metric-card accent-blue">
          <div className="metric-top">
            <span>เวลาที่ใช้</span>
            <span className="metric-icon">◷</span>
          </div>
          <div className="metric-value metric-compact">
            {formatDuration(totalMinutes)}
          </div>
          <div className="metric-foot">
            จากงบ {project.budget_hours || "—"} ชั่วโมง
          </div>
        </article>
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
        <article className="metric-card accent-violet">
          <div className="metric-top">
            <span>งาน</span>
            <span className="metric-icon">✓</span>
          </div>
          <div className="metric-value">
            {completed}
            <span className="metric-unit">/ {tasks.length}</span>
          </div>
          <div className="metric-foot">เสร็จแล้ว {taskProgress}%</div>
        </article>
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
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>งาน</h2>
                <p>สร้าง ปิดงาน แก้ไข และเรียงลำดับงานในโปรเจกต์</p>
              </div>
              <button
                className="button button-primary"
                type="button"
                onClick={() => openTask()}
              >
                ＋ เพิ่ม งาน
              </button>
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
          </section>

          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>รายการเวลาล่าสุด</h2>
                <p>เวลาล่าสุดที่บันทึกในโปรเจกต์</p>
              </div>
              <Link className="text-button" to="/time-tracker">
                ดูทั้งหมด →
              </Link>
            </div>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>รายละเอียด</th>
                    <th>วันที่</th>
                    <th>ระยะเวลา</th>
                    <th>คิดค่าบริการ</th>
                    <th>มูลค่า</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.slice(0, 6).map((entry) => (
                    <tr key={entry.id}>
                      <td>
                        <strong>
                          {entry.description || "ไม่มีรายละเอียด"}
                        </strong>
                      </td>
                      <td>{formatDate(entry.started_at)}</td>
                      <td>{formatDuration(entry.duration_minutes)}</td>
                      <td>{entry.billable ? "ใช่" : "ไม่"}</td>
                      <td>
                        {formatMoney(calculateTimeValue(entry), entry.currency)}
                      </td>
                    </tr>
                  ))}
                  {entries.length === 0 && (
                    <tr>
                      <td colSpan={5}>ยังไม่มีรายการเวลา</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <aside className="panel">
          <div className="panel-heading">
            <div>
              <h2>รายละเอียดโปรเจกต์</h2>
              <p>ขอบเขต ราคา และกำหนดการ</p>
            </div>
          </div>
          <div className="detail-list">
            <div className="detail-item">
              <span>ลูกค้า</span>
              <Link to={`/clients/${client?.id}`}>
                <strong>{client?.company_name || client?.name}</strong>
              </Link>
            </div>
            <div className="detail-item">
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
            </div>
            <div className="detail-item">
              <span>วันที่เริ่ม</span>
              <strong>{formatDate(project.start_date)}</strong>
            </div>
            <div className="detail-item">
              <span>วันที่สิ้นสุด</span>
              <strong>{formatDate(project.end_date)}</strong>
            </div>
            {effectiveRate !== null && (
              <div className="detail-item">
                <span>อัตราต่อชั่วโมงโดยเฉลี่ย</span>
                <strong>
                  {formatMoney(effectiveRate, project.currency)}/ชม.
                </strong>
              </div>
            )}
            <div className="detail-item">
              <span>ความคืบหน้างาน</span>
              <div className="progress-label">
                <span>
                  {completed}/{tasks.length}
                </span>
                <strong>{taskProgress}%</strong>
              </div>
              <div className="progress-track">
                <span
                  style={{
                    width: `${taskProgress}%`,
                    background: project.color,
                  }}
                />
              </div>
            </div>
          </div>
        </aside>
      </div>

      <Modal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title={taskForm.id ? "แก้ไข Task" : "เพิ่ม Task"}
      >
        <TaskForm
          value={taskForm}
          error={formError}
          onChange={setTaskForm}
          onSubmit={saveTask}
          onCancel={() => setModalOpen(false)}
        />
      </Modal>
    </div>
  );
}

export default ProjectDetailPage;
