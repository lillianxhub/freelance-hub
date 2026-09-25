import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import Modal from '../components/Modal'
import PageHeader from '../components/PageHeader'
import { EmptyState, ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import type { FinanceEntry, FinanceType, ResourceInput } from '../types/domain'
import { calculateTimeValue, formatDate, formatMoney } from '../utils/formatters'
import { effectiveInvoiceStatus, invoiceBalance } from '../utils/invoices'

const localDate = () => {
  const date = new Date()
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

type FinanceForm = Omit<ResourceInput<'finance_entries'>, 'amount' | 'project_id'> & {
  amount: number | ''
  project_id: string
}

interface FinanceFilters {
  type: 'ALL' | FinanceType
  project: string
  from: string
  to: string
}

const emptyEntry = (): FinanceForm => ({ type: 'EXPENSE', category: '', amount: '', entry_date: localDate(), project_id: '', notes: '', currency: 'THB' })

function FinancesPage() {
  const { data, loading, error, refresh, save, remove } = useWorkspace()
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState(emptyEntry())
  const [formError, setFormError] = useState('')
  const [filters, setFilters] = useState<FinanceFilters>({ type: 'ALL', project: 'ALL', from: '', to: '' })

  const entries = useMemo(() => (data?.finance_entries || [])
    .filter((entry) => filters.type === 'ALL' || entry.type === filters.type)
    .filter((entry) => filters.project === 'ALL' || entry.project_id === filters.project)
    .filter((entry) => !filters.from || entry.entry_date >= filters.from)
    .filter((entry) => !filters.to || entry.entry_date <= filters.to)
    .sort((a, b) => b.entry_date.localeCompare(a.entry_date)), [data?.finance_entries, filters])

  if (loading) return <LoadingState label="กำลังโหลดข้อมูลการเงิน..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const income = data.finance_entries.filter((entry) => entry.type === 'INCOME').reduce((sum, entry) => sum + Number(entry.amount), 0)
  const expenses = data.finance_entries.filter((entry) => entry.type === 'EXPENSE').reduce((sum, entry) => sum + Number(entry.amount), 0)
  const paidInvoices = data.invoices.filter((invoice) => invoice.status === 'PAID').reduce((sum, invoice) => sum + Number(invoice.amount_paid || invoice.total), 0)
  const outstanding = data.invoices.filter((invoice) => ['ISSUED', 'OVERDUE'].includes(effectiveInvoiceStatus(invoice))).reduce((sum, invoice) => sum + invoiceBalance(invoice), 0)
  const unbilled = data.time_entries.filter((entry) => entry.billable && !entry.invoice_id && entry.ended_at).reduce((sum, entry) => sum + calculateTimeValue(entry), 0)
  const net = income + paidInvoices - expenses

  const openForm = (entry: FinanceEntry | null = null) => {
    setForm(entry ? { ...entry, project_id: entry.project_id || '' } : { ...emptyEntry(), currency: data.profiles[0]?.currency || 'THB' })
    setFormError('')
    setModalOpen(true)
  }

  const handleChange = (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!form.category.trim() || Number(form.amount) <= 0) {
      setFormError('กรุณาระบุหมวดหมู่และจำนวนเงินที่มากกว่า 0')
      return
    }
    await save('finance_entries', { ...form, category: form.category.trim(), amount: Number(form.amount), project_id: form.project_id || null })
    setModalOpen(false)
  }

  return (
    <div className="page-view">
      <PageHeader eyebrow="Manage / Finances" title="Finances" description="ติดตามกระแสเงินสด รายได้ ค่าใช้จ่าย และเงินที่รอเรียกเก็บ" actions={<button className="button button-primary" type="button" onClick={() => openForm()}>＋ เพิ่มรายการ</button>} />

      <div className="summary-grid">
        <article className="metric-card accent-green"><div className="metric-top"><span>เงินรับแล้ว</span><span className="metric-icon">↗</span></div><div className="metric-value metric-compact">{formatMoney(income + paidInvoices)}</div><div className="metric-foot">Invoice ที่ชำระแล้ว + รายได้อื่น</div></article>
        <article className="metric-card accent-red"><div className="metric-top"><span>ค่าใช้จ่าย</span><span className="metric-icon">↘</span></div><div className="metric-value metric-compact">{formatMoney(expenses)}</div><div className="metric-foot">ค่าใช้จ่ายที่บันทึกทั้งหมด</div></article>
        <article className="metric-card accent-orange"><div className="metric-top"><span>ยอดค้างรับ</span><span className="metric-icon">◷</span></div><div className="metric-value metric-compact">{formatMoney(outstanding)}</div><div className="metric-foot">Invoice ที่ออกแล้วแต่ยังไม่ครบ</div></article>
        <article className="metric-card accent-violet"><div className="metric-top"><span>เวลาที่ยังไม่วางบิล</span><span className="metric-icon">◇</span></div><div className="metric-value metric-compact">{formatMoney(unbilled)}</div><div className="metric-foot">มูลค่าจาก billable time</div></article>
      </div>

      <section className="panel finance-overview">
        <div><span>กำไรสุทธิโดยประมาณ</span><strong className={net >= 0 ? 'positive-money' : 'negative-money'}>{formatMoney(net)}</strong><small>คำนวณจากเงินที่รับแล้ว หักค่าใช้จ่ายที่บันทึก</small></div>
        <div className="finance-bars"><div><span>รายรับ</span><i><b style={{ width: `${Math.min(100, ((income + paidInvoices) / Math.max(1, income + paidInvoices + expenses)) * 100)}%` }} /></i></div><div><span>รายจ่าย</span><i className="expense"><b style={{ width: `${Math.min(100, (expenses / Math.max(1, income + paidInvoices + expenses)) * 100)}%` }} /></i></div></div>
      </section>

      <section className="panel">
        <div className="panel-heading"><div><h2>Income & expenses</h2><p>รายการที่เพิ่มด้วยตนเอง แยกจากการรับชำระ Invoice</p></div><strong>{entries.length} รายการ</strong></div>
        <div className="entry-filters finance-filters">
          <select value={filters.type} onChange={(event) => setFilters((current) => ({ ...current, type: event.target.value as FinanceFilters['type'] }))}><option value="ALL">ทุกประเภท</option><option value="INCOME">รายได้</option><option value="EXPENSE">ค่าใช้จ่าย</option></select>
          <select value={filters.project} onChange={(event) => setFilters((current) => ({ ...current, project: event.target.value }))}><option value="ALL">ทุกโปรเจกต์</option>{data.projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select>
          <input type="date" value={filters.from} aria-label="จากวันที่" onChange={(event) => setFilters((current) => ({ ...current, from: event.target.value }))} />
          <input type="date" value={filters.to} aria-label="ถึงวันที่" onChange={(event) => setFilters((current) => ({ ...current, to: event.target.value }))} />
        </div>
        {entries.length === 0 ? <EmptyState icon="฿" title="ยังไม่มีรายการการเงิน" description="เพิ่มรายได้หรือค่าใช้จ่าย หรือเปลี่ยนตัวกรองเพื่อดูข้อมูล" /> : (
          <div className="table-wrap"><table className="data-table"><thead><tr><th>รายการ</th><th>วันที่</th><th>โปรเจกต์</th><th>ประเภท</th><th>จำนวนเงิน</th><th /></tr></thead><tbody>{entries.map((entry) => {
            const project = data.projects.find((item) => item.id === entry.project_id)
            return <tr key={entry.id}><td><div className="table-primary"><span className={`finance-icon ${entry.type.toLowerCase()}`}>{entry.type === 'INCOME' ? '↗' : '↘'}</span><span><strong>{entry.category}</strong><small>{entry.notes || 'ไม่มีรายละเอียด'}</small></span></div></td><td>{formatDate(entry.entry_date)}</td><td>{project?.name || 'ทั่วไป'}</td><td><span className={`type-badge ${entry.type === 'INCOME' ? 'income' : 'expense'}`}>{entry.type === 'INCOME' ? 'รายได้' : 'ค่าใช้จ่าย'}</span></td><td><strong className={entry.type === 'INCOME' ? 'positive-money' : 'negative-money'}>{entry.type === 'INCOME' ? '+' : '−'}{formatMoney(entry.amount, entry.currency)}</strong></td><td><div className="table-actions"><button className="mini-button" type="button" onClick={() => openForm(entry)}>แก้ไข</button><button className="mini-button" type="button" onClick={() => remove('finance_entries', entry.id)}>ลบ</button></div></td></tr>
          })}</tbody></table></div>
        )}
      </section>

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={form.id ? 'แก้ไขรายการ' : 'เพิ่มรายได้หรือค่าใช้จ่าย'}>
        <form onSubmit={handleSubmit}>
          {formError && <p className="form-error">{formError}</p>}
          <div className="form-grid">
            <div className="form-field"><label htmlFor="finance-type">ประเภท</label><select id="finance-type" name="type" value={form.type} onChange={handleChange}><option value="INCOME">รายได้</option><option value="EXPENSE">ค่าใช้จ่าย</option></select></div>
            <div className="form-field"><label htmlFor="finance-category">หมวดหมู่</label><input id="finance-category" name="category" value={form.category} onChange={handleChange} placeholder="เช่น Software, Consulting" required /></div>
            <div className="form-field"><label htmlFor="finance-amount">จำนวนเงิน</label><input id="finance-amount" name="amount" type="number" min="0.01" step="0.01" value={form.amount} onChange={handleChange} required /></div>
            <div className="form-field"><label htmlFor="finance-date">วันที่</label><input id="finance-date" name="entry_date" type="date" value={form.entry_date} onChange={handleChange} required /></div>
            <div className="form-field full"><label htmlFor="finance-project">โปรเจกต์ (ไม่บังคับ)</label><select id="finance-project" name="project_id" value={form.project_id} onChange={handleChange}><option value="">รายการทั่วไป</option>{data.projects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></div>
            <div className="form-field full"><label htmlFor="finance-notes">รายละเอียด</label><textarea id="finance-notes" name="notes" value={form.notes} onChange={handleChange} /></div>
          </div>
          <div className="form-actions"><button className="button button-secondary" type="button" onClick={() => setModalOpen(false)}>ยกเลิก</button><button className="button button-primary" type="submit">บันทึกรายการ</button></div>
        </form>
      </Modal>
    </div>
  )
}

export default FinancesPage
