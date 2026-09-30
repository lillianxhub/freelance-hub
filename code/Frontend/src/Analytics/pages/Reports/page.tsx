import { Card } from '../../../components/ui/card'
import { Button } from '../../../components/ui/button'
import { Label } from '../../../components/ui/label'
import { Input } from '../../../components/ui/input'
import { NativeSelect } from '../../../components/ui/native-select'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../../components/ui/table'
import { ChartContainer, ChartTooltip, ChartTooltipContent } from '../../../components/ui/chart'
import { Progress } from '../../../components/ui/progress'
import { useMemo, useState } from "react";
import { FiActivity, FiAward, FiBarChart2, FiClock, FiDownload, FiSun, FiTrendingDown, FiTrendingUp } from "react-icons/fi";
import {
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
} from "recharts";
import PageHeader from "../../../components/PageHeader";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useAnalytics } from "../../useAnalytics";
import {
  downloadCsv,
  groupTimeBy,
  inDateRange,
  summarizeTime,
} from "../../../utils/analytics";
import { formatDuration } from "../../../utils/formatters";

const reportNow = new Date();
const reportTo = new Date(
  reportNow.getTime() - reportNow.getTimezoneOffset() * 60000,
)
  .toISOString()
  .slice(0, 10);
const reportFrom = `${reportTo.slice(0, 8)}01`;

function ReportsPage() {
  const { data, loading, error, refresh } = useAnalytics();
  const [range, setRange] = useState({ from: reportFrom, to: reportTo });
  const [clientId, setClientId] = useState("ALL");
  const [projectId, setProjectId] = useState("ALL");

  const availableProjects = useMemo(
    () => (data?.projects || []).filter((project) => clientId === "ALL" || project.client_id === clientId),
    [clientId, data?.projects],
  );
  const filteredTime = useMemo(() => (data?.time_entries || []).filter((entry) => {
    const project = data?.projects.find((item) => item.id === entry.project_id);
    return Boolean(entry.ended_at) &&
      (clientId === "ALL" || project?.client_id === clientId) &&
      (projectId === "ALL" || entry.project_id === projectId) &&
      inDateRange(entry.started_at, range.from, range.to);
  }), [clientId, data?.projects, data?.time_entries, projectId, range]);

  if (loading) return <LoadingState label="กำลังประมวลผลReports..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const summary = summarizeTime(filteredTime);
  const projectGroups = groupTimeBy(
    filteredTime,
    (entry) => entry.project_id,
  ).map((group) => ({
    ...group,
    name:
      data.projects.find((project) => project.id === group.key)?.name ||
      "ไม่ทราบโปรเจกต์",
    hours: Number((group.minutes / 60).toFixed(2)),
  }));
  const clientGroups = groupTimeBy(filteredTime, (entry) => {
    const project = data.projects.find((item) => item.id === entry.project_id);
    return project?.client_id || "UNKNOWN";
  }).map((group) => ({
    ...group,
    name: data.clients.find((client) => client.id === group.key)?.company_name ||
      data.clients.find((client) => client.id === group.key)?.name ||
      "ไม่ระบุลูกค้า",
    hours: Number((group.minutes / 60).toFixed(2)),
  }));
  const daily = [
    ...filteredTime
      .reduce((map, entry) => {
        const day = entry.started_at.slice(0, 10);
        const current = map.get(day) || {
          date: day,
          hours: 0,
        };
        current.hours += Number(entry.duration_minutes) / 60;
        map.set(day, current);
        return map;
      }, new Map())
      .values(),
  ]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((item) => ({
      ...item,
      label: new Intl.DateTimeFormat("th-TH", {
        day: "numeric",
        month: "short",
      }).format(new Date(item.date)),
    }));
  const topProjects = projectGroups[0];
  const fromDate = new Date(`${range.from}T00:00:00`);
  const toDate = new Date(`${range.to}T00:00:00`);
  const rangeDays = Math.max(1, Math.round((toDate.getTime() - fromDate.getTime()) / 86400000) + 1);
  const previousToDate = new Date(fromDate);
  previousToDate.setDate(previousToDate.getDate() - 1);
  const previousFromDate = new Date(previousToDate);
  previousFromDate.setDate(previousFromDate.getDate() - rangeDays + 1);
  const dateKey = (date: Date) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  const previousEntries = data.time_entries.filter((entry) => {
    const project = data.projects.find((item) => item.id === entry.project_id);
    return Boolean(entry.ended_at) &&
      (clientId === "ALL" || project?.client_id === clientId) &&
      (projectId === "ALL" || entry.project_id === projectId) &&
      inDateRange(entry.started_at, dateKey(previousFromDate), dateKey(previousToDate));
  });
  const previousMinutes = previousEntries.reduce((sum, entry) => sum + Number(entry.duration_minutes || 0), 0);
  const productivityTrend = previousMinutes
    ? ((summary.trackedMinutes - previousMinutes) / previousMinutes) * 100
    : summary.trackedMinutes ? 100 : 0;
  const weekdayTotals = filteredTime.reduce((totals, entry) => {
    const label = new Intl.DateTimeFormat("th-TH", { weekday: "long" }).format(new Date(entry.started_at));
    totals.set(label, (totals.get(label) || 0) + Number(entry.duration_minutes || 0));
    return totals;
  }, new Map<string, number>());
  const mostProductiveDay = [...weekdayTotals.entries()].sort((a, b) => b[1] - a[1])[0];
  const periodTotals = filteredTime.reduce((totals, entry) => {
    const hour = new Date(entry.started_at).getHours();
    const period = hour < 6 ? "กลางคืน" : hour < 12 ? "ช่วงเช้า" : hour < 18 ? "ช่วงบ่าย" : "ช่วงเย็น";
    totals.set(period, (totals.get(period) || 0) + Number(entry.duration_minutes || 0));
    return totals;
  }, new Map<string, number>());
  const mostActivePeriod = [...periodTotals.entries()].sort((a, b) => b[1] - a[1])[0];

  const exportTime = () =>
    downloadCsv(`time-report-${range.from}-${range.to}.csv`, [
      [
        "วันที่",
        "ลูกค้า",
        "โปรเจกต์",
        "งาน",
        "รายละเอียด",
        "ชั่วโมง",
      ],
      ...filteredTime.map((entry) => {
        const project = data.projects.find(
          (item) => item.id === entry.project_id,
        );
        const client = data.clients.find(
          (item) => item.id === project?.client_id,
        );
        const task = data.tasks.find((item) => item.id === entry.task_id);
        return [
          entry.started_at.slice(0, 10),
          client?.company_name || client?.name,
          project?.name,
          task?.name,
          entry.description,
          (Number(entry.duration_minutes) / 60).toFixed(2),
        ];
      }),
    ]);
  /* const exportRevenue = () =>
    downloadCsv(`revenue-report-${range.from}-${range.to}.csv`, [
      [
        "เลขที่ใบแจ้งหนี้",
        "ลูกค้า",
        "วันที่ออกเอกสาร",
        "วันครบกำหนด",
        "สถานะ",
        "ยอดก่อนภาษี",
        "ภาษี",
        "ยอดรวม",
        "รับเงินแล้ว",
        "ยอดคงเหลือ",
        "สกุลเงิน",
      ],
      ...filteredInvoices.map((invoice) => {
        const client = data.clients.find(
          (item) => item.id === invoice.client_id,
        );
        return [
          invoice.invoice_number,
          client?.company_name || client?.name,
          invoice.issue_date,
          invoice.due_date,
          invoice.status,
          invoice.subtotal,
          invoice.tax_amount,
          invoice.total,
          invoice.amount_paid,
          Number(invoice.total) - Number(invoice.amount_paid || 0),
          invoice.currency,
        ];
      }),
    ]); */

  return (
    <div className="page-view">
      <PageHeader
        eyebrow="จัดการ / รายงาน"
        title="รายงานและข้อมูลสรุป"
        description="ดูเวลาและประสิทธิภาพจากข้อมูลจริง"
        actions={
          <>
            <Button variant="outline"
              className="button button-secondary"
              type="button"
              onClick={exportTime}
            >
              <FiDownload aria-hidden="true" /> เวลา CSV
            </Button>
            {/* <button
              className="button button-primary"
              type="button"
              onClick={exportRevenue}
            >
              <FiDownload aria-hidden="true" /> ดาวน์โหลดรายรับ CSV
            </button> */}
          </>
        }
      />
      <Card asChild><section className="panel report-controls">
        <div className="form-field">
          <Label htmlFor="report-from">จากวันที่</Label>
          <Input
            id="report-from"
            type="date"
            value={range.from}
            onChange={(event) =>
              setRange((current) => ({ ...current, from: event.target.value }))
            }
          />
        </div>
        <div className="form-field">
          <Label htmlFor="report-to">ถึงวันที่</Label>
          <Input
            id="report-to"
            type="date"
            value={range.to}
            onChange={(event) =>
              setRange((current) => ({ ...current, to: event.target.value }))
            }
          />
        </div>
        <div className="form-field">
          <Label htmlFor="report-client">ลูกค้า</Label>
          <NativeSelect
            id="report-client"
            value={clientId}
            onChange={(event) => {
              setClientId(event.target.value);
              setProjectId("ALL");
            }}
          >
            <option value="ALL">ลูกค้าทั้งหมด</option>
            {data.clients.map((client) => (
              <option value={client.id} key={client.id}>{client.company_name || client.name}</option>
            ))}
          </NativeSelect>
        </div>
        <div className="form-field">
          <Label htmlFor="report-project">โปรเจกต์</Label>
          <NativeSelect id="report-project" value={projectId} onChange={(event) => setProjectId(event.target.value)}>
            <option value="ALL">โปรเจกต์ทั้งหมด</option>
            {availableProjects.map((project) => (
              <option value={project.id} key={project.id}>{project.name}</option>
            ))}
          </NativeSelect>
        </div>
        {/* <div className="form-field">
          <label htmlFor="report-currency">สกุลเงิน</label>
          <select
            id="report-currency"
            value={currency}
            onChange={(event) => setCurrency(event.target.value)}
          >
            {currencies.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </div> */}
        {/* <p>จำนวนเงินจะแยกตามสกุลเงินเพื่อป้องกันการรวมยอดที่ผิดพลาด</p> */}
      </section></Card>
      <div className="summary-grid">
        <Card asChild><article className="metric-card accent-blue">
          <div className="metric-top">
            <span>ชั่วโมงที่บันทึกทั้งหมด</span>
            <span className="metric-icon"><FiClock aria-hidden="true" /></span>
          </div>
          <div className="metric-value">
            {(summary.trackedMinutes / 60).toFixed(1)}
            <span className="metric-unit">ชม.</span>
          </div>
          <div className="metric-foot">
            จาก {filteredTime.length} รายการเวลา
          </div>
        </article></Card>
        <Card asChild><article className="metric-card accent-violet">
          <div className="metric-top">
            <span>ชั่วโมงเฉลี่ยต่อวัน</span>
            <span className="metric-icon"><FiBarChart2 aria-hidden="true" /></span>
          </div>
          <div className="metric-value">
            {(summary.trackedMinutes / 60 / rangeDays).toFixed(1)}
            <span className="metric-unit">ชม.</span>
          </div>
          <div className="metric-foot">
            คำนวณจาก {rangeDays} วันในช่วงที่เลือก
          </div>
        </article></Card>
        <Card asChild><article className="metric-card accent-orange">
          <div className="metric-top">
            <span>โปรเจกต์ที่ใช้เวลาสูงสุด</span>
            <span className="metric-icon"><FiAward aria-hidden="true" /></span>
          </div>
          <div className="metric-value metric-compact">{topProjects?.name || "—"}</div>
          <div className="metric-foot">{topProjects ? `${topProjects.hours.toFixed(1)} ชั่วโมง` : "ยังไม่มีข้อมูล"}</div>
        </article></Card>
        <Card asChild><article className="metric-card accent-green">
          <div className="metric-top">
            <span>แนวโน้มประสิทธิภาพ</span>
            <span className="metric-icon">{productivityTrend >= 0 ? <FiTrendingUp aria-hidden="true" /> : <FiTrendingDown aria-hidden="true" />}</span>
          </div>
          <div className="metric-value">{productivityTrend >= 0 ? "+" : ""}{productivityTrend.toFixed(0)}<span className="metric-unit">%</span></div>
          <div className="metric-foot">เทียบกับช่วงเวลาก่อนหน้าที่มีจำนวนวันเท่ากัน</div>
        </article></Card>
      </div>
      <div className="dashboard-grid report-chart-grid">
        <Card asChild><section className="panel chart-panel">
          <div className="panel-heading">
            <div>
              <h2>แนวโน้มชั่วโมงทำงาน</h2>
              <p>ชั่วโมงทำงานในแต่ละวันของช่วงที่เลือก</p>
            </div>
          </div>
          {daily.length ? (
            <ChartContainer className="h-[270px] w-full aspect-auto" config={{ hours: { label: 'ชั่วโมง', color: '#4F6BFF' } }}>
              <BarChart data={daily}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  vertical={false}
                  stroke="#E2E8F0"
                />
                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 'var(--font-size-sm)' }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 'var(--font-size-sm)' }}
                  axisLine={false}
                  tickLine={false}
                />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Bar
                  dataKey="hours"
                  name="ชั่วโมง"
                  fill="var(--color-hours)"
                  radius={[5, 5, 0, 0]}
                />
              </BarChart>
            </ChartContainer>
          ) : (
            <p className="inline-empty">ไม่มีข้อมูลในช่วงวันที่นี้</p>
          )}
        </section></Card>
        <Card asChild><section className="panel chart-panel">
          <div className="panel-heading">
            <div>
              <h2>สัดส่วนเวลาตามลูกค้าและโปรเจกต์</h2>
              <p>ดูว่างานส่วนใหญ่อยู่กับลูกค้าหรือโปรเจกต์ใด</p>
            </div>
          </div>
          <div className="report-breakdown">
            <div>
              <h3>ตามลูกค้า</h3>
              <div className="breakdown-list">
                {clientGroups.length ? clientGroups.map((group) => (
                  <div key={group.key}><span>{group.name}</span><strong>{group.hours.toFixed(1)} ชม.</strong></div>
                )) : <p className="inline-empty">ไม่มีข้อมูลลูกค้า</p>}
              </div>
            </div>
            <div>
              <h3>ตามโปรเจกต์</h3>
              <div className="breakdown-list">
                {projectGroups.length ? projectGroups.map((group) => (
                  <div key={group.key}><span>{group.name}</span><strong>{group.hours.toFixed(1)} ชม.</strong></div>
                )) : <p className="inline-empty">ไม่มีข้อมูลโปรเจกต์</p>}
              </div>
            </div>
          </div>
        </section></Card>
      </div>
      <div className="lower-grid">
        <Card asChild><section className="panel">
          <div className="panel-heading">
            <div>
              <h2>วิเคราะห์โปรเจกต์</h2>
              <p>เปรียบเทียบชั่วโมงที่บันทึกกับเป้าหมายของแต่ละโปรเจกต์</p>
            </div>
          </div>
          <div className="table-wrap">
            <Table className="data-table">
              <TableHeader>
                <TableRow>
                  <TableHead>โปรเจกต์</TableHead>
                  <TableHead>ชั่วโมงที่บันทึก</TableHead>
                  <TableHead>ชั่วโมงเป้าหมาย</TableHead>
                  <TableHead>ความคืบหน้า</TableHead>
                  <TableHead>สถานะ</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {projectGroups.map((group) => {
                  const project = data.projects.find(
                    (item) => item.id === group.key,
                  );
                  const targetHours = Number(project?.budget_hours || 0);
                  const progress = targetHours ? (group.hours / targetHours) * 100 : 0;
                  return (
                    <TableRow key={group.key}>
                      <TableCell>
                        <div className="table-primary">
                          <span
                            className="color-dot"
                            style={{ "--dot-color": project?.color }}
                          />
                          <strong>{group.name}</strong>
                        </div>
                      </TableCell>
                      <TableCell>{group.hours.toFixed(1)} ชม.</TableCell>
                      <TableCell>{targetHours ? `${targetHours.toFixed(1)} ชม.` : "ไม่กำหนด"}</TableCell>
                      <TableCell>
                        <div className="table-progress"><Progress value={progress} /><strong>{targetHours ? `${progress.toFixed(0)}%` : "—"}</strong></div>
                      </TableCell>
                      <TableCell>{targetHours ? progress >= 100 ? "ถึงเป้าหมายแล้ว" : progress >= 80 ? "ใกล้ถึงเป้าหมาย" : "กำลังดำเนินการ" : "ยังไม่มีเป้าหมาย"}</TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        </section></Card>
        <Card asChild><section className="panel insight-card">
          <div className="panel-heading">
            <div>
              <h2>รูปแบบการทำงาน</h2>
              <p>ช่วงเวลาที่ทำงานได้มากและแนวโน้มเมื่อเทียบช่วงก่อนหน้า</p>
            </div>
          </div>
          <div className="insight-list">
            <div>
              <span><FiAward aria-hidden="true" /></span>
              <p>
                <strong>วันที่ทำงานได้มากที่สุด</strong>
                {mostProductiveDay
                  ? `${mostProductiveDay[0]} · ${formatDuration(mostProductiveDay[1])}`
                  : "ยังไม่มีข้อมูล"}
              </p>
            </div>
            <div>
              <span><FiSun aria-hidden="true" /></span>
              <p>
                <strong>ช่วงเวลาที่ทำงานมากที่สุด</strong>
                {mostActivePeriod
                  ? `${mostActivePeriod[0]} · ${formatDuration(mostActivePeriod[1])}`
                  : "ยังไม่มีข้อมูล"}
              </p>
            </div>
            <div>
              <span><FiActivity aria-hidden="true" /></span>
              <p>
                <strong>แนวโน้มชั่วโมงทำงาน</strong>
                {productivityTrend === 0
                  ? "ชั่วโมงทำงานใกล้เคียงกับช่วงก่อนหน้า"
                  : `${productivityTrend > 0 ? "เพิ่มขึ้น" : "ลดลง"} ${Math.abs(productivityTrend).toFixed(0)}% จากช่วงก่อนหน้า`}
              </p>
            </div>
          </div>
        </section></Card>
      </div>
    </div>
  );
}

export default ReportsPage;
