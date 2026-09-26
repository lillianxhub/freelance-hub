import type { ClientFormProps } from '../../types/clientsPage'

export default function ClientForm({ value, error, saving, onChange, onSubmit, onCancel }: ClientFormProps) {
  return <form onSubmit={onSubmit}>
    {error && <p className="form-error">{error}</p>}
    <div className="form-grid">
      <div className="form-field"><label htmlFor="client-name">ชื่อผู้ติดต่อ</label><input id="client-name" name="name" value={value.name} onChange={onChange} placeholder="ชื่อ-นามสกุล" /></div>
      <div className="form-field"><label htmlFor="company-name">ชื่อบริษัท</label><input id="company-name" name="company_name" value={value.company_name} onChange={onChange} placeholder="บริษัท จำกัด" /></div>
      <div className="form-field"><label htmlFor="client-email">อีเมล</label><input id="client-email" name="email" type="email" value={value.email} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="client-phone">โทรศัพท์</label><input id="client-phone" name="phone" value={value.phone} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="client-tax">เลขประจำตัวผู้เสียภาษี</label><input id="client-tax" name="tax_id" value={value.tax_id} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="client-status">สถานะ</label><select id="client-status" name="status" value={value.status} onChange={onChange}><option value="ACTIVE">ใช้งานอยู่</option><option value="ARCHIVED">เก็บถาวร</option></select></div>
      <div className="form-field full"><label htmlFor="client-address">ที่อยู่</label><textarea id="client-address" name="address" value={value.address} onChange={onChange} /></div>
      <div className="form-field full"><label htmlFor="client-notes">หมายเหตุ</label><textarea id="client-notes" name="notes" value={value.notes} onChange={onChange} /></div>
    </div>
    <div className="form-actions"><button className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'กำลังบันทึก...' : 'บันทึกลูกค้า'}</button></div>
  </form>
}
