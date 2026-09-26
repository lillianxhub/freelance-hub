import type { ProjectFormProps } from '../../types/projectsPage'

export default function ProjectForm({ value, clients, error, saving, onChange, onSubmit, onCancel }: ProjectFormProps) {
  return <form onSubmit={onSubmit}>
    {error && <p className="form-error">{error}</p>}
    <div className="form-grid">
      <div className="form-field"><label htmlFor="project-name">ชื่อโปรเจกต์</label><input id="project-name" name="name" value={value.name} onChange={onChange} required /></div>
      <div className="form-field"><label htmlFor="project-client">ลูกค้า</label><select id="project-client" name="client_id" value={value.client_id} onChange={onChange} required>{clients.filter((client) => client.status === 'ACTIVE' || client.id === value.client_id).map((client) => <option key={client.id} value={client.id}>{client.company_name || client.name}</option>)}</select></div>
      <div className="form-field"><label htmlFor="project-status">สถานะ</label><select id="project-status" name="status" value={value.status} onChange={onChange}><option value="PLANNED">วางแผน</option><option value="ACTIVE">กำลังดำเนินการ</option><option value="ON_HOLD">พักงาน</option><option value="COMPLETED">เสร็จสิ้น</option><option value="ARCHIVED">เก็บถาวร</option></select></div>
      <div className="form-field"><label htmlFor="project-billing">รูปแบบราคา</label><select id="project-billing" name="billing_type" value={value.billing_type} onChange={onChange}><option value="HOURLY">รายชั่วโมง</option><option value="FIXED_PRICE">เหมาจ่าย</option></select></div>
      {value.billing_type === 'HOURLY' ? <div className="form-field"><label htmlFor="hourly-rate">อัตราต่อชั่วโมง</label><input id="hourly-rate" name="hourly_rate" type="number" min="0" step="0.01" value={value.hourly_rate} onChange={onChange} required /></div> : <div className="form-field"><label htmlFor="fixed-price">มูลค่างาน</label><input id="fixed-price" name="fixed_price" type="number" min="0" step="0.01" value={value.fixed_price} onChange={onChange} required /></div>}
      <div className="form-field"><label htmlFor="budget-hours">งบชั่วโมง</label><input id="budget-hours" name="budget_hours" type="number" min="0" step="0.5" value={value.budget_hours} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="start-date">วันที่เริ่ม</label><input id="start-date" name="start_date" type="date" value={value.start_date || ''} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="end-date">วันที่สิ้นสุด</label><input id="end-date" name="end_date" type="date" value={value.end_date || ''} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="project-color">สีโปรเจกต์</label><input id="project-color" name="color" type="color" value={value.color} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="project-currency">สกุลเงิน</label><select id="project-currency" name="currency" value={value.currency} onChange={onChange}><option value="THB">THB</option><option value="USD">USD</option><option value="EUR">EUR</option></select></div>
      <div className="form-field full"><label htmlFor="project-description">รายละเอียด</label><textarea id="project-description" name="description" value={value.description} onChange={onChange} /></div>
    </div>
    <div className="form-actions"><button className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'กำลังบันทึก...' : 'บันทึกโปรเจกต์'}</button></div>
  </form>
}
