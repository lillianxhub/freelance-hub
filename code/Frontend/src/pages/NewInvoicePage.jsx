import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PageHeader from '../components/PageHeader'
import { ErrorState, LoadingState } from '../components/ViewState'
import { useWorkspace } from '../contexts/workspaceContextValue'
import { formatDate, formatMoney } from '../utils/formatters'

const baseDate = new Date()
const dateValue = (date = baseDate) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
const dueDate = new Date(baseDate.getTime() + 14 * 86400000)

function NewInvoicePage() {
  const navigate = useNavigate()
  const { data, loading, error, refresh, save } = useWorkspace()
  const [clientId, setClientId] = useState('')
  const [projectId, setProjectId] = useState('ALL')
  const [selectedTimeIds, setSelectedTimeIds] = useState([])
  const [manualItems, setManualItems] = useState([{ id: crypto.randomUUID(), description: '', quantity: 1, unit_price: 0 }])
  const [form, setForm] = useState({ issue_date: dateValue(), due_date: dateValue(dueDate), tax_rate: 7, discount_amount: 0, notes: 'ขอบคุณที่ไว้วางใจใช้บริการ' })
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const activeClients = useMemo(() => (data?.clients || []).filter((client) => client.status === 'ACTIVE'), [data?.clients])
  const effectiveClientId = clientId || activeClients[0]?.id || ''
  const clientProjects = useMemo(() => (data?.projects || []).filter((project) => project.client_id === effectiveClientId), [data?.projects, effectiveClientId])
  const eligibleTime = useMemo(() => {
    const projectIds = new Set(clientProjects.map((project) => project.id))
    return (data?.time_entries || []).filter((entry) => entry.billable && entry.ended_at && !entry.invoice_id && projectIds.has(entry.project_id) && (projectId === 'ALL' || entry.project_id === projectId))
  }, [clientProjects, data?.time_entries, projectId])

  if (loading) return <LoadingState label="กำลังเตรียม Invoice..." />
  if (error) return <ErrorState message={error} onRetry={refresh} />

  const client = data.clients.find((item) => item.id === effectiveClientId)
  const selectedTime = data.time_entries.filter((entry) => selectedTimeIds.includes(entry.id))
  const timeItems = selectedTime.map((entry) => {
    const project = data.projects.find((item) => item.id === entry.project_id)
    const quantity = Number(entry.duration_minutes) / 60
    return { time_entry_id: entry.id, description: `${project?.name || 'Project'} — ${entry.description || 'Billable time'}`, quantity, unit_price: Number(entry.rate_snapshot), amount: quantity * Number(entry.rate_snapshot) }
  })
  const validManualItems = manualItems.filter((item) => item.description.trim() && Number(item.quantity) > 0)
    .map((item) => ({ ...item, quantity: Number(item.quantity), unit_price: Number(item.unit_price), amount: Number(item.quantity) * Number(item.unit_price), time_entry_id: null }))
  const invoiceItems = [...timeItems, ...validManualItems]
  const subtotal = invoiceItems.reduce((sum, item) => sum + item.amount, 0)
  const discount = Math.min(subtotal, Math.max(0, Number(form.discount_amount) || 0))
  const taxAmount = (subtotal - discount) * ((Number(form.tax_rate) || 0) / 100)
  const total = subtotal - discount + taxAmount

  const updateManualItem = (id, field, value) => setManualItems((items) => items.map((item) => item.id === id ? { ...item, [field]: value } : item))
  const toggleTime = (id) => setSelectedTimeIds((ids) => ids.includes(id) ? ids.filter((item) => item !== id) : [...ids, id])

  const createInvoiceNumber = () => {
    const year = new Date(form.issue_date).getFullYear()
    const maximum = data.invoices.reduce((max, invoice) => {
      const value = Number(invoice.invoice_number.match(/(\d+)$/)?.[1] || 0)
      return Math.max(max, value)
    }, 1000)
    return `INV-${year}-${maximum + 1}`
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!client || invoiceItems.length === 0) {
      setFormError('กรุณาเลือกลูกค้าและเพิ่มอย่างน้อย 1 รายการ')
      return
    }
    if (form.due_date < form.issue_date) {
      setFormError('วันครบกำหนดต้องไม่ก่อนวันที่ออก Invoice')
      return
    }
    setSaving(true)
    setFormError('')
    try {
      const invoiceId = crypto.randomUUID()
      const profile = data.profiles[0]
      await save('invoices', {
        id: invoiceId, client_id: client.id, project_id: projectId === 'ALL' ? null : projectId,
        invoice_number: createInvoiceNumber(), issue_date: form.issue_date, due_date: form.due_date, status: 'DRAFT',
        currency: profile?.currency || 'THB', subtotal, discount_amount: discount, tax_rate: Number(form.tax_rate) || 0,
        tax_amount: taxAmount, total, amount_paid: 0, notes: form.notes,
        seller_snapshot: { name: profile?.full_name, email: profile?.email, phone: profile?.phone, address: profile?.address, tax_id: profile?.tax_id, bank_name: profile?.bank_name, bank_account_name: profile?.bank_account_name, bank_account_number: profile?.bank_account_number },
        client_snapshot: { name: client.company_name || client.name, contact_name: client.name, email: client.email, phone: client.phone, address: client.address, tax_id: client.tax_id },
      })
      for (const [index, item] of invoiceItems.entries()) {
        const { id: _temporaryId, ...cleanItem } = item
        await save('invoice_items', { ...cleanItem, invoice_id: invoiceId, sort_order: index + 1 })
        if (item.time_entry_id) {
          const entry = data.time_entries.find((candidate) => candidate.id === item.time_entry_id)
          await save('time_entries', { ...entry, invoice_id: invoiceId })
        }
      }
      navigate(`/invoices/${invoiceId}`)
    } catch (err) {
      setFormError(err.message || 'ไม่สามารถสร้าง Invoice ได้')
      setSaving(false)
    }
  }

  return (
    <div className="page-view">
      <Link className="back-link" to="/invoices">← กลับไป Invoices</Link>
      <PageHeader eyebrow="Invoices / New" title="Create invoice" description="รวมเวลาที่ยังไม่วางบิลและรายการกำหนดเองไว้ในเอกสารเดียว" />
      <form className="invoice-editor" onSubmit={handleSubmit}>
        {formError && <p className="form-error">{formError}</p>}
        <div className="invoice-editor-grid">
          <div className="section-stack">
            <section className="panel"><div className="panel-heading"><div><h2>1. ลูกค้าและกำหนดชำระ</h2><p>ข้อมูลจะถูก snapshot เมื่อสร้างเอกสาร</p></div></div><div className="form-grid">
              <div className="form-field full"><label htmlFor="invoice-client">ลูกค้า</label><select id="invoice-client" value={effectiveClientId} onChange={(event) => { setClientId(event.target.value); setProjectId('ALL'); setSelectedTimeIds([]) }}>{activeClients.map((item) => <option key={item.id} value={item.id}>{item.company_name || item.name}</option>)}</select></div>
              <div className="form-field"><label htmlFor="invoice-issue">วันที่ออก</label><input id="invoice-issue" type="date" value={form.issue_date} onChange={(event) => setForm((current) => ({ ...current, issue_date: event.target.value }))} /></div>
              <div className="form-field"><label htmlFor="invoice-due">ครบกำหนด</label><input id="invoice-due" type="date" value={form.due_date} onChange={(event) => setForm((current) => ({ ...current, due_date: event.target.value }))} /></div>
            </div></section>

            <section className="panel"><div className="panel-heading"><div><h2>2. เลือกเวลาที่ยังไม่วางบิล</h2><p>เลือกได้เฉพาะเวลาของลูกค้ารายนี้และไม่เคยอยู่ใน Invoice</p></div><select className="select-button" value={projectId} onChange={(event) => { setProjectId(event.target.value); setSelectedTimeIds([]) }}><option value="ALL">ทุกโปรเจกต์</option>{clientProjects.map((project) => <option key={project.id} value={project.id}>{project.name}</option>)}</select></div>
              {eligibleTime.length === 0 ? <p className="inline-empty">ไม่มีเวลาที่ยังไม่วางบิลสำหรับตัวเลือกนี้</p> : <div className="invoice-time-list">{eligibleTime.map((entry) => { const project = data.projects.find((item) => item.id === entry.project_id); const hours = Number(entry.duration_minutes) / 60; return <label key={entry.id} className="invoice-time-row"><input type="checkbox" checked={selectedTimeIds.includes(entry.id)} onChange={() => toggleTime(entry.id)} /><span><strong>{entry.description || project?.name}</strong><small>{project?.name} · {formatDate(entry.started_at)}</small></span><strong>{hours.toFixed(2)} ชม.</strong><b>{formatMoney(hours * Number(entry.rate_snapshot), entry.currency)}</b></label> })}</div>}
            </section>

            <section className="panel"><div className="panel-heading"><div><h2>3. รายการเพิ่มเติม</h2><p>เพิ่มค่าบริการแบบเหมาจ่ายหรือค่าใช้จ่ายอื่น</p></div><button className="mini-button" type="button" onClick={() => setManualItems((items) => [...items, { id: crypto.randomUUID(), description: '', quantity: 1, unit_price: 0 }])}>＋ เพิ่มแถว</button></div>
              <div className="manual-items"><div className="manual-item header"><span>รายละเอียด</span><span>จำนวน</span><span>ราคาต่อหน่วย</span><span>รวม</span><span /></div>{manualItems.map((item) => <div className="manual-item" key={item.id}><input value={item.description} onChange={(event) => updateManualItem(item.id, 'description', event.target.value)} placeholder="รายละเอียดบริการ" /><input type="number" min="0.01" step="0.01" value={item.quantity} onChange={(event) => updateManualItem(item.id, 'quantity', event.target.value)} /><input type="number" min="0" step="0.01" value={item.unit_price} onChange={(event) => updateManualItem(item.id, 'unit_price', event.target.value)} /><strong>{formatMoney(Number(item.quantity) * Number(item.unit_price))}</strong><button type="button" aria-label="ลบรายการ" onClick={() => setManualItems((items) => items.filter((candidate) => candidate.id !== item.id))}>×</button></div>)}</div>
            </section>
          </div>

          <aside className="panel invoice-summary-card"><div><span>Invoice number</span><strong>{createInvoiceNumber()}</strong></div><div className="invoice-client-summary"><span>เรียกเก็บจาก</span><strong>{client?.company_name || client?.name}</strong><small>{client?.email}</small></div><div className="form-field"><label htmlFor="invoice-tax">ภาษี (%)</label><input id="invoice-tax" type="number" min="0" max="100" step="0.01" value={form.tax_rate} onChange={(event) => setForm((current) => ({ ...current, tax_rate: event.target.value }))} /></div><div className="form-field"><label htmlFor="invoice-discount">ส่วนลด</label><input id="invoice-discount" type="number" min="0" step="0.01" value={form.discount_amount} onChange={(event) => setForm((current) => ({ ...current, discount_amount: event.target.value }))} /></div><div className="invoice-totals"><p><span>ยอดก่อนภาษี</span><strong>{formatMoney(subtotal)}</strong></p><p><span>ส่วนลด</span><strong>−{formatMoney(discount)}</strong></p><p><span>ภาษี {Number(form.tax_rate) || 0}%</span><strong>{formatMoney(taxAmount)}</strong></p><p className="grand-total"><span>ยอดรวม</span><strong>{formatMoney(total)}</strong></p></div><div className="form-field"><label htmlFor="invoice-notes">หมายเหตุ</label><textarea id="invoice-notes" value={form.notes} onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))} /></div><button className="button button-primary wide" type="submit" disabled={saving}>{saving ? 'กำลังสร้าง...' : 'บันทึกเป็น Draft'}</button><Link className="button button-secondary wide" to="/invoices">ยกเลิก</Link></aside>
        </div>
      </form>
    </div>
  )
}

export default NewInvoicePage
