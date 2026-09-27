import { useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import PageHeader from "../../../components/PageHeader";
import { ErrorState, LoadingState } from "../../../components/ViewState";
import { useWorkspace } from "../../../Workspace/useWorkspace";
import {
  downloadCsv,
  groupTimeBy,
  inDateRange,
  summarizeTime,
} from "../../../utils/analytics";
import { formatDuration, formatMoney } from "../../../utils/formatters";
import {
  effectiveInvoiceStatus,
  invoiceBalance,
} from "../../../utils/invoices";
import ProjectTimeChart from "../../components/ProjectTimeChart";

const reportNow = new Date();
const reportTo = new Date(
  reportNow.getTime() - reportNow.getTimezoneOffset() * 60000,
)
  .toISOString()
  .slice(0, 10);
const reportFrom = `${reportTo.slice(0, 8)}01`;

function ReportsPage() {
  const { data, loading, error, refresh } = useWorkspace();
  const [range, setRange] = useState({ from: reportFrom, to: reportTo });
  const [currency, setCurrency] = useState("THB");

  const currencies = useMemo(
    () => [
      ...new Set(
        [
          ...(data?.time_entries || []).map((entry) => entry.currency),
          ...(data?.invoices || []).map((invoice) => invoice.currency),
          ...(data?.finance_entries || []).map((entry) => entry.currency),
        ].filter(Boolean),
      ),
    ],
    [data],
  );

  const filteredTime = useMemo(
    () =>
      (data?.time_entries || []).filter(
        (entry) =>
          entry.ended_at &&
          entry.currency === currency &&
          inDateRange(entry.started_at, range.from, range.to),
      ),
    [currency, data?.time_entries, range],
  );
  const filteredInvoices = useMemo(
    () =>
      (data?.invoices || []).filter(
        (invoice) =>
          invoice.currency === currency &&
          inDateRange(invoice.issue_date, range.from, range.to),
      ),
    [currency, data?.invoices, range],
  );
  const filteredPayments = useMemo(
    () =>
      (data?.payments || []).filter(
        (payment) =>
          payment.currency === currency &&
          inDateRange(payment.paid_at, range.from, range.to),
      ),
    [currency, data?.payments, range],
  );

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
    billableHours: Number((group.billableMinutes / 60).toFixed(2)),
  }));
  const daily = [
    ...filteredTime
      .reduce((map, entry) => {
        const day = entry.started_at.slice(0, 10);
        const current = map.get(day) || {
          date: day,
          billable: 0,
          nonBillable: 0,
        };
        current[entry.billable ? "billable" : "nonBillable"] +=
          Number(entry.duration_minutes) / 60;
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
  const invoiced = filteredInvoices
    .filter((invoice) =>
      ["ISSUED", "OVERDUE"].includes(effectiveInvoiceStatus(invoice, reportTo)),
    )
    .reduce((sum, invoice) => sum + Number(invoice.total), 0);
  const paid = filteredPayments.reduce(
    (sum, payment) => sum + Number(payment.amount),
    0,
  );
  const overdue = filteredInvoices
    .filter(
      (invoice) => effectiveInvoiceStatus(invoice, reportTo) === "OVERDUE",
    )
    .reduce((sum, invoice) => sum + invoiceBalance(invoice), 0);
  const trackedDays = new Set(
    filteredTime.map((entry) => entry.started_at.slice(0, 10)),
  ).size;
  const topProjects = projectGroups[0];

  const exportTime = () =>
    downloadCsv(`time-report-${range.from}-${range.to}.csv`, [
      [
        "วันที่",
        "ลูกค้า",
        "โปรเจกต์",
        "งาน",
        "รายละเอียด",
        "ชั่วโมง",
        "คิดค่าบริการ",
        "อัตรา",
        "สกุลเงิน",
        "มูลค่า",
        "สถานะใบแจ้งหนี้",
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
          entry.billable ? "ใช่" : "ไม่ใช่",
          entry.rate_snapshot,
          entry.currency,
          entry.billable
            ? (
                (Number(entry.duration_minutes) / 60) *
                Number(entry.rate_snapshot)
              ).toFixed(2)
            : "0.00",
          entry.invoice_id ? "Invoicesd" : "ยังไม่วางบิล",
        ];
      }),
    ]);
  const exportRevenue = () =>
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
    ]);

  return (
    <div className="page-view">
      <PageHeader
        eyebrow="จัดการ / รายงาน"
        title="รายงานและข้อมูลสรุป"
        description="ดูเวลา รายได้ และประสิทธิภาพจากข้อมูลจริง"
        actions={
          <>
            <button
              className="button button-secondary"
              type="button"
              onClick={exportTime}
            >
              ↓ เวลา CSV
            </button>
            <button
              className="button button-primary"
              type="button"
              onClick={exportRevenue}
            >
              ↓ ดาวน์โหลดรายรับ CSV
            </button>
          </>
        }
      />
      <section className="panel report-controls">
        <div className="form-field">
          <label htmlFor="report-from">จากวันที่</label>
          <input
            id="report-from"
            type="date"
            value={range.from}
            onChange={(event) =>
              setRange((current) => ({ ...current, from: event.target.value }))
            }
          />
        </div>
        <div className="form-field">
          <label htmlFor="report-to">ถึงวันที่</label>
          <input
            id="report-to"
            type="date"
            value={range.to}
            onChange={(event) =>
              setRange((current) => ({ ...current, to: event.target.value }))
            }
          />
        </div>
        <div className="form-field">
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
        </div>
        <p>จำนวนเงินจะแยกตามสกุลเงินเพื่อป้องกันการรวมยอดที่ผิดพลาด</p>
      </section>
      <div className="summary-grid">
        <article className="metric-card accent-blue">
          <div className="metric-top">
            <span>เวลาทั้งหมด</span>
            <span className="metric-icon">◷</span>
          </div>
          <div className="metric-value">
            {(summary.trackedMinutes / 60).toFixed(1)}
            <span className="metric-unit">ชม.</span>
          </div>
          <div className="metric-foot">
            เฉลี่ย{" "}
            {trackedDays
              ? (summary.trackedMinutes / 60 / trackedDays).toFixed(1)
              : "0.0"}{" "}
            ชม./วันที่ทำTask
          </div>
        </article>
        <article className="metric-card accent-violet">
          <div className="metric-top">
            <span>สัดส่วนเวลาที่คิดเงินได้</span>
            <span className="metric-icon">%</span>
          </div>
          <div className="metric-value">
            {summary.utilization.toFixed(0)}
            <span className="metric-unit">%</span>
          </div>
          <div className="metric-foot">
            {formatDuration(summary.billableMinutes)} ที่เรียกเก็บได้
          </div>
        </article>
        <article className="metric-card accent-orange">
          <div className="metric-top">
            <span>ยังไม่วางบิล</span>
            <span className="metric-icon">◇</span>
          </div>
          <div className="metric-value metric-compact">
            {formatMoney(summary.unbilledValue, currency)}
          </div>
          <div className="metric-foot">เวลาที่ยังไม่อยู่ใน ใบแจ้งหนี้</div>
        </article>
        <article className="metric-card accent-green">
          <div className="metric-top">
            <span>รับเงินแล้ว</span>
            <span className="metric-icon">✓</span>
          </div>
          <div className="metric-value metric-compact">
            {formatMoney(paid, currency)}
          </div>
          <div className="metric-foot">
            Invoicesd {formatMoney(invoiced, currency)} · Overdue{" "}
            {formatMoney(overdue, currency)}
          </div>
        </article>
      </div>
      <div className="dashboard-grid">
        <section className="panel chart-panel">
          <div className="panel-heading">
            <div>
              <h2>เวลาทำงานรายวัน</h2>
              <p>เปรียบเทียบเวลาที่คิดเงินได้และไม่ได้คิดเงิน</p>
            </div>
          </div>
          {daily.length ? (
            <ResponsiveContainer width="100%" height={270}>
              <BarChart data={daily}>
                <CartesianGrid
                  strokeDasharray="3 3"
                  vertical={false}
                  stroke="#e8ebf1"
                />
                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 9 }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 9 }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip />
                <Bar
                  dataKey="billable"
                  name="billable"
                  stackId="time"
                  fill="#3867f4"
                  radius={[5, 5, 0, 0]}
                />
                <Bar
                  dataKey="nonBillable"
                  name="ไม่billable"
                  stackId="time"
                  fill="#cfd7e7"
                  radius={[5, 5, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="inline-empty">ไม่มีข้อมูลในช่วงวันที่นี้</p>
          )}
        </section>
        <section className="panel chart-panel">
          <div className="panel-heading">
            <div>
              <h2>เวลาตามโปรเจกต์</h2>
              <p>ชั่วโมงรวมของแต่ละโปรเจกต์</p>
            </div>
          </div>
          <ProjectTimeChart data={projectGroups} />
        </section>
      </div>
      <div className="lower-grid">
        <section className="panel">
          <div className="panel-heading">
            <div>
              <h2>ผลงานตามโปรเจกต์</h2>
              <p>เวลา มูลค่า และ อัตราต่อชั่วโมงโดยเฉลี่ย</p>
            </div>
          </div>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>โปรเจกต์</th>
                  <th>เวลารวม</th>
                  <th>คิดค่าบริการ</th>
                  <th>มูลค่าเกิดขึ้น</th>
                  <th>อัตราเฉลี่ย</th>
                </tr>
              </thead>
              <tbody>
                {projectGroups.map((group) => {
                  const project = data.projects.find(
                    (item) => item.id === group.key,
                  );
                  const effective =
                    project?.billing_type === "FIXED_PRICE" && group.hours
                      ? Number(project.fixed_price) / group.hours
                      : group.hours
                        ? group.value / group.hours
                        : 0;
                  return (
                    <tr key={group.key}>
                      <td>
                        <div className="table-primary">
                          <span
                            className="color-dot"
                            style={{ "--dot-color": project?.color }}
                          />
                          <strong>{group.name}</strong>
                        </div>
                      </td>
                      <td>{group.hours.toFixed(1)} ชม.</td>
                      <td>{group.billableHours.toFixed(1)} ชม.</td>
                      <td>
                        {formatMoney(
                          project?.billing_type === "FIXED_PRICE"
                            ? project.fixed_price
                            : group.value,
                          currency,
                        )}
                      </td>
                      <td>{formatMoney(effective, currency)}/ชม.</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
        <section className="panel insight-card">
          <div className="panel-heading">
            <div>
              <h2>ข้อสังเกตการทำงาน</h2>
              <p>ข้อสังเกตจากช่วงวันที่ที่เลือก</p>
            </div>
          </div>
          <div className="insight-list">
            <div>
              <span>★</span>
              <p>
                <strong>โปรเจกต์ที่ใช้เวลาสูงสุด</strong>
                {topProjects
                  ? `${topProjects.name} · ${topProjects.hours.toFixed(1)} ชั่วโมง`
                  : "ยังไม่มีข้อมูล"}
              </p>
            </div>
            <div>
              <span>◎</span>
              <p>
                <strong>อัตราเวลาที่เรียกเก็บได้</strong>
                {summary.utilization >= 75
                  ? "อยู่ในระดับดี รักษาสมดุลนี้ต่อไป"
                  : "ลองลดงาน ไม่คิดค่าบริการ หรือทบทวนขอบเขตงาน"}
              </p>
            </div>
            <div>
              <span>฿</span>
              <p>
                <strong>โอกาสเรียกเก็บเงิน</strong>มี{" "}
                {formatMoney(summary.unbilledValue, currency)}{" "}
                ที่พร้อมนำไปสร้างใบแจ้งหนี้
              </p>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

export default ReportsPage;
