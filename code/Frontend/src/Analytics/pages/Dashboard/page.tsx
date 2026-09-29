import { useState } from "react";
import { Link } from "react-router-dom";
import {
    FiBarChart2,
    FiBriefcase,
    FiCalendar,
    FiClock,
    FiPlay,
    FiTrendingDown,
    FiTrendingUp,
} from "react-icons/fi";
import PageHeader from "../../../components/PageHeader";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useAnalytics } from "../../useAnalytics";
import type { TimeEntry } from "../../../types/timeTracking";
import { formatDuration } from "../../../utils/formatters";
import TimerPanel from "../../../TimeTracking/components/TimerPanel";
import SummaryCard from "../../components/SummaryCard";
import ProductivityChart from "../../components/ProductivityChart";
import ProjectTimeChart from "../../components/ProjectTimeChart";
import RecentActivity from "../../components/RecentActivity";

type DashboardRange = "TODAY" | "WEEK" | "MONTH";

function localDateKey(date: Date): string {
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
        .toISOString()
        .slice(0, 10);
}

function dateAtOffset(date: Date, offset: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + offset);
    return result;
}

function minutesInRange(
    entries: TimeEntry[],
    from: string,
    to: string,
): number {
    return entries
        .filter((entry) => {
            const key = entry.started_at.slice(0, 10);
            return key >= from && key <= to;
        })
        .reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0);
}

function trend(current: number, previous: number): number {
    if (!previous) return current ? 100 : 0;
    return ((current - previous) / previous) * 100;
}

function DashboardPage() {
    const analytics = useAnalytics();
    const { data, loading, error, refresh } = analytics;
    const [selectedRange, setSelectedRange] = useState<DashboardRange>("WEEK");

    if (loading) return <LoadingState label="กำลังสรุป Dashboard..." />;
    if (error) return <ErrorState message={error} onRetry={refresh} />;

    const now = new Date();
    const today = localDateKey(now);
    const weekday = now.getDay() || 7;
    const weekStart = localDateKey(dateAtOffset(now, 1 - weekday));
    const previousWeekStart = localDateKey(dateAtOffset(now, 1 - weekday - 7));
    const previousWeekEnd = localDateKey(dateAtOffset(now, -weekday));
    const monthStart = `${today.slice(0, 8)}01`;
    const previousMonthEndDate = new Date(now.getFullYear(), now.getMonth(), 0);
    const previousMonthStart = `${previousMonthEndDate.getFullYear()}-${String(previousMonthEndDate.getMonth() + 1).padStart(2, "0")}-01`;
    const previousMonthEnd = localDateKey(previousMonthEndDate);
    const finishedTime = data.time_entries.filter((entry) => entry.ended_at);
    const todayMinutes = minutesInRange(finishedTime, today, today);
    const yesterday = localDateKey(dateAtOffset(now, -1));
    const yesterdayMinutes = minutesInRange(finishedTime, yesterday, yesterday);
    const weekMinutes = minutesInRange(finishedTime, weekStart, today);
    const previousWeekMinutes = minutesInRange(
        finishedTime,
        previousWeekStart,
        previousWeekEnd,
    );
    const monthMinutes = minutesInRange(finishedTime, monthStart, today);
    const previousMonthMinutes = minutesInRange(
        finishedTime,
        previousMonthStart,
        previousMonthEnd,
    );
    const profile = data.profiles[0];
    const selectedFrom =
        selectedRange === "TODAY"
            ? today
            : selectedRange === "WEEK"
              ? weekStart
              : monthStart;
    const selectedEntries = finishedTime.filter((entry) => {
        const date = entry.started_at.slice(0, 10);
        return date >= selectedFrom && date <= today;
    });

    const chartData = Array.from({ length: 7 }, (_, index) => {
        const date = dateAtOffset(now, index - 6);
        const key = localDateKey(date);
        const entries = finishedTime.filter(
            (entry) => entry.started_at.slice(0, 10) === key,
        );
        return {
            key,
            day: new Intl.DateTimeFormat("th-TH", { weekday: "short" }).format(
                date,
            ),
            billable: Number(
                (
                    entries
                        .filter((entry) => entry.billable)
                        .reduce(
                            (sum, entry) =>
                                sum + Number(entry.duration_minutes),
                            0,
                        ) / 60
                ).toFixed(2),
            ),
            total: Number(
                (
                    entries.reduce(
                        (sum, entry) => sum + Number(entry.duration_minutes),
                        0,
                    ) / 60
                ).toFixed(2),
            ),
        };
    });

    const activeProjects = data.projects.filter(
        (project) => project.status === "ACTIVE",
    );
    const projectTime = activeProjects
        .map((project) => ({
            key: project.id,
            name: project.name,
            hours: Number(
                (
                    selectedEntries
                        .filter((entry) => entry.project_id === project.id)
                        .reduce(
                            (sum, entry) =>
                                sum + Number(entry.duration_minutes || 0),
                            0,
                        ) / 60
                ).toFixed(2),
            ),
        }))
        .filter((project) => project.hours > 0)
        .sort((a, b) => b.hours - a.hours);
    const projectProgress = activeProjects
        .map((project) => {
            const entries = finishedTime.filter(
                (entry) => entry.project_id === project.id,
            );
            const minutes = entries.reduce(
                (sum, entry) => sum + Number(entry.duration_minutes || 0),
                0,
            );
            const percent = project.budget_hours
                ? (minutes / 60 / Number(project.budget_hours)) * 100
                : 0;
            return { ...project, minutes, percent };
        })
        .sort((a, b) => b.percent - a.percent);
    const recentEntries = [...selectedEntries]
        .sort((a, b) => Date.parse(b.started_at) - Date.parse(a.started_at))
        .slice(0, 5);

    return (
        <div className="page-view">
            <PageHeader
                eyebrow="พื้นที่ทำงาน / ภาพรวม"
                title={`สวัสดี, ${(profile?.full_name || "ฟรีแลนซ์").split(" ")[0]}`}
                // description="สรุปเวลา โปรเจกต์ และความคืบหน้าจากข้อมูลการทำงานจริง"
                /* actions={
                    <>
                        <Link className="button button-secondary" to="/reports">
                            ดูรายงาน
                        </Link>
                        <Link
                            className="button button-primary"
                            to="/time-tracker"
                        >
                            <FiPlay aria-hidden="true" /> เริ่มจับเวลา
                        </Link>
                    </>
                } */
            />
            <div
                className="dashboard-range-bar"
                aria-label="เลือกช่วงเวลาของภาพรวม"
            >
                {(
                    [
                        ["TODAY", "วันนี้"],
                        ["WEEK", "สัปดาห์นี้"],
                        ["MONTH", "เดือนนี้"],
                    ] as const
                ).map(([value, label]) => (
                    <button
                        className={selectedRange === value ? "active" : ""}
                        type="button"
                        key={value}
                        onClick={() => setSelectedRange(value)}
                    >
                        {label}
                    </button>
                ))}
            </div>
            <div className="summary-grid dashboard-summary-grid">
                <SummaryCard
                    label="วันนี้"
                    icon={<FiClock aria-hidden="true" />}
                    value={(todayMinutes / 60).toFixed(1)}
                    unit="ชม."
                    accent="blue"
                    foot={
                        <>
                            <span
                                className={
                                    trend(todayMinutes, yesterdayMinutes) >= 0
                                        ? "trend-positive"
                                        : "negative-money"
                                }
                            >
                                {trend(todayMinutes, yesterdayMinutes) >= 0 ? (
                                    <FiTrendingUp aria-hidden="true" />
                                ) : (
                                    <FiTrendingDown aria-hidden="true" />
                                )}{" "}
                                {Math.abs(
                                    trend(todayMinutes, yesterdayMinutes),
                                ).toFixed(0)}
                                %
                            </span>{" "}
                            เทียบเมื่อวาน
                        </>
                    }
                />
                <SummaryCard
                    label="สัปดาห์นี้"
                    icon={<FiCalendar aria-hidden="true" />}
                    value={(weekMinutes / 60).toFixed(1)}
                    unit="ชม."
                    accent="green"
                    foot={
                        <>
                            <span
                                className={
                                    trend(weekMinutes, previousWeekMinutes) >= 0
                                        ? "trend-positive"
                                        : "negative-money"
                                }
                            >
                                {trend(weekMinutes, previousWeekMinutes) >=
                                0 ? (
                                    <FiTrendingUp aria-hidden="true" />
                                ) : (
                                    <FiTrendingDown aria-hidden="true" />
                                )}{" "}
                                {Math.abs(
                                    trend(weekMinutes, previousWeekMinutes),
                                ).toFixed(0)}
                                %
                            </span>{" "}
                            เทียบสัปดาห์ก่อน
                        </>
                    }
                />
                <SummaryCard
                    label="เดือนนี้"
                    icon={<FiBarChart2 aria-hidden="true" />}
                    value={(monthMinutes / 60).toFixed(1)}
                    unit="ชม."
                    accent="violet"
                    foot={
                        <>
                            <span
                                className={
                                    trend(monthMinutes, previousMonthMinutes) >=
                                    0
                                        ? "trend-positive"
                                        : "negative-money"
                                }
                            >
                                {trend(monthMinutes, previousMonthMinutes) >=
                                0 ? (
                                    <FiTrendingUp aria-hidden="true" />
                                ) : (
                                    <FiTrendingDown aria-hidden="true" />
                                )}{" "}
                                {Math.abs(
                                    trend(monthMinutes, previousMonthMinutes),
                                ).toFixed(0)}
                                %
                            </span>{" "}
                            เทียบเดือนก่อน
                        </>
                    }
                />
                <SummaryCard
                    label="โปรเจกต์ที่กำลังทำ"
                    icon={<FiBriefcase aria-hidden="true" />}
                    value={activeProjects.length}
                    unit="โปรเจกต์"
                    accent="orange"
                    foot={`${data.projects.length} โปรเจกต์ทั้งหมด`}
                />
            </div>

            <div className="dashboard-grid dashboard-chart-grid">
                <section className="panel chart-panel">
                    <div className="panel-heading">
                        <div>
                            <h2>ชั่วโมงทำงานรายสัปดาห์</h2>
                            <p>เวลารวมและเวลาที่คิดค่าบริการใน 7 วันล่าสุด</p>
                        </div>
                        <Link className="mini-button text-link" to="/reports">
                            ดูทั้งหมด
                        </Link>
                    </div>
                    <ProductivityChart data={chartData} />
                </section>
                {/* <section className="panel cashflow-card">
          <div className="panel-heading">
            <div>
              <h2>สรุปรายรับ</h2>
              <p>สรุปยอดในCurrency {currency}</p>
            </div>
            <Link className="mini-button text-link" to="/finances">
              จัดการ
            </Link>
          </div>
          <div className="cashflow-total">
            <span>รายได้ที่รับแล้ว</span>
            <strong>{formatMoney(paid, currency)}</strong>
          </div>
          <div className="summary-list">
            <div>
              <span>
                <i className="legend-dot blue" />
                ยังไม่วางบิล
              </span>
              <strong>{formatMoney(unbilled, currency)}</strong>
            </div>
            <div>
              <span>
                <i className="legend-dot violet" />
                ออกใบแจ้งหนี้แล้ว
              </span>
              <strong>{formatMoney(invoiced, currency)}</strong>
            </div>
            <div>
              <span>
                <i className="legend-dot green" />
                รับเงินแล้ว
              </span>
              <strong>{formatMoney(paid, currency)}</strong>
            </div>
            <div>
              <span>
                <i className="legend-dot red" />
                เกินกำหนด
              </span>
              <strong className={overdue ? "negative-money" : ""}>
                {formatMoney(overdue, currency)}
              </strong>
            </div>
          </div>
        </section> */}
            </div>

            <div className="lower-grid dashboard-lower-grid">
                <TimerPanel workspace={analytics} />
                <section className="panel chart-panel">
                    <div className="panel-heading">
                        <div>
                            <h2>เวลาตามโปรเจกต์</h2>
                            <p>สัดส่วนชั่วโมงในช่วงเวลาที่เลือก</p>
                        </div>
                    </div>
                    <ProjectTimeChart data={projectTime} />
                </section>
            </div>

            <section className="panel dashboard-progress-panel">
                <div className="panel-heading">
                    <div>
                        <h2>ความคืบหน้าเทียบเป้าหมาย</h2>
                        <p>
                            ชั่วโมงที่ใช้เทียบกับชั่วโมงเป้าหมายของโปรเจกต์ที่กำลังทำ
                        </p>
                    </div>
                    <Link className="mini-button text-link" to="/projects">
                        ดูทั้งหมด
                    </Link>
                </div>
                <div className="dashboard-projects dashboard-project-progress">
                    {projectProgress.length ? (
                        projectProgress.map((project) => {
                            const client = data.clients.find(
                                (item) => item.id === project.client_id,
                            );
                            return (
                                <Link
                                    to={`/projects/${project.id}`}
                                    key={project.id}
                                >
                                    <span
                                        className="project-dot"
                                        style={{ background: project.color }}
                                    />
                                    <span>
                                        <strong>{project.name}</strong>
                                        <small>
                                            {client?.company_name ||
                                                client?.name ||
                                                "ไม่ระบุลูกค้า"}
                                        </small>
                                        <i className="progress-track">
                                            <b
                                                style={{
                                                    width: `${Math.min(100, project.percent)}%`,
                                                    background:
                                                        project.percent >= 100
                                                            ? "#dc4c64"
                                                            : project.color,
                                                }}
                                            />
                                        </i>
                                    </span>
                                    <span>
                                        <strong>
                                            {formatDuration(project.minutes)} /{" "}
                                            {project.budget_hours
                                                ? `${project.budget_hours} ชม.`
                                                : "ไม่กำหนด"}
                                        </strong>
                                        <small>
                                            {project.percent.toFixed(0)}%
                                            ของเป้าหมาย
                                        </small>
                                    </span>
                                </Link>
                            );
                        })
                    ) : (
                        <p className="inline-empty">
                            ยังไม่มีโปรเจกต์ที่กำลังทำ
                        </p>
                    )}
                </div>
            </section>

            {/*
      <div className="lower-grid dashboard-lower-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>โปรเจกต์ที่กำลังทำ</h2>
              <p>ติดตามความคืบหน้าและการใช้งบประมาณ</p>
            </div>
            <Link className="mini-button text-link" to="/projects">
              ดูทั้งหมด
            </Link>
          </div>
          <div className="dashboard-projects">
            {projectProgress.length ? (
              projectProgress.map((project) => {
                const client = data.clients.find(
                  (item) => item.id === project.client_id,
                );
                return (
                  <Link to={`/projects/${project.id}`} key={project.id}>
                    <span
                      className="project-dot"
                      style={{ background: project.color }}
                    />
                    <span>
                      <strong>{project.name}</strong>
                      <small>{client?.company_name || client?.name}</small>
                      <i className="progress-track">
                        <b
                          style={{
                            width: `${Math.min(100, project.percent)}%`,
                            background:
                              project.percent >= 100
                                ? "#dc4c64"
                                : project.percent >= 80
                                  ? "#e97834"
                                  : project.color,
                          }}
                        />
                      </i>
                    </span>
                    <span>
                      <strong>{project.percent.toFixed(0)}%</strong>
                      <small>{formatDuration(project.minutes)}</small>
                    </span>
                    {project.percent >= 80 && (
                      <em>
                        {project.percent >= 100 ? "เกินงบ" : "ใกล้เต็มงบ"}
                      </em>
                    )}
                  </Link>
                );
              })
            ) : (
              <p className="inline-empty">ยังไม่มีโปรเจกต์ที่กำลังทำ</p>
            )}
          </div>
        </section>
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>งานที่ต้องทำต่อ</h2>
              <p>งานที่ยังไม่เสร็จ เรียงตามกำหนดส่ง</p>
            </div>
          </div>
          <div className="dashboard-tasks">
            {upcomingTasks.length ? (
              upcomingTasks.map((task) => {
                const project = data.projects.find(
                  (item) => item.id === task.project_id,
                );
                return (
                  <Link to={`/projects/${project?.id}`} key={task.id}>
                    <span className="task-state-dot" />
                    <span>
                      <strong>{task.name}</strong>
                      <small>{project?.name}</small>
                    </span>
                    <span>
                      <StatusBadge status={task.status} />
                      <small>{formatDate(task.due_date)}</small>
                    </span>
                  </Link>
                );
              })
            ) : (
              <p className="inline-empty">ไม่มีงานค้างอยู่</p>
            )}
          </div>
        </section>
      </div>
      */}

            <section className="panel recent-time-panel">
                <div className="panel-heading">
                    <div>
                        <h2>รายการเวลาล่าสุด</h2>
                        <p>กิจกรรมล่าสุดใน พื้นที่ทำงาน</p>
                    </div>
                    <Link className="mini-button text-link" to="/time-tracker">
                        ดู บันทึกเวลา
                    </Link>
                </div>
                <RecentActivity
                    entries={recentEntries}
                    projects={data.projects}
                />
            </section>
        </div>
    );
}

export default DashboardPage;
