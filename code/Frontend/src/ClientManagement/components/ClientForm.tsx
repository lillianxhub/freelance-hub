import { useEffect, useState, type ChangeEvent } from 'react'
import type { ClientFormProps } from '../../types/clientsPage'
import { loadThaiAddressData, type ThaiProvince } from '../../services/thaiAddress'

export default function ClientForm({ value, error, saving, onChange, onFieldsChange, onSubmit, onCancel }: ClientFormProps) {
  const [provinces, setProvinces] = useState<ThaiProvince[]>([])
  const [addressLoading, setAddressLoading] = useState(true)
  const [addressError, setAddressError] = useState('')

  useEffect(() => {
    let active = true
    loadThaiAddressData()
      .then((items) => {
        if (active) setProvinces(items)
      })
      .catch(() => {
        if (active) setAddressError('ไม่สามารถโหลดข้อมูลจังหวัดได้')
      })
      .finally(() => {
        if (active) setAddressLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const selectedProvince = provinces.find((item) => item.name_th === value.province)
  const districts = selectedProvince?.districts || []
  const selectedDistrict = districts.find((item) => item.name_th === value.district)
  const subDistricts = selectedDistrict?.sub_districts || []
  const selectedSubDistrict = subDistricts.find((item) => item.name_th === value.sub_district)

  const handleProvinceChange = (event: ChangeEvent<HTMLSelectElement>) => {
    onFieldsChange({ province: event.target.value, district: '', sub_district: '', postal_code: '' })
  }

  const handleDistrictChange = (event: ChangeEvent<HTMLSelectElement>) => {
    onFieldsChange({ district: event.target.value, sub_district: '', postal_code: '' })
  }

  const handleSubDistrictChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const subDistrict = subDistricts.find((item) => item.name_th === event.target.value)
    onFieldsChange({ sub_district: event.target.value, postal_code: subDistrict ? String(subDistrict.zip_code) : '' })
  }

  return <form onSubmit={onSubmit}>
    {error && <p className="form-error">{error}</p>}
    <div className="form-grid">
      <div className="form-field"><label htmlFor="client-name">ชื่อผู้ติดต่อ</label><input id="client-name" name="name" value={value.name} onChange={onChange} placeholder="ชื่อ-นามสกุล" /></div>
      <div className="form-field"><label htmlFor="company-name">ชื่อบริษัท</label><input id="company-name" name="company_name" value={value.company_name} onChange={onChange} placeholder="บริษัท จำกัด" /></div>
      <div className="form-field"><label htmlFor="client-email">อีเมล</label><input id="client-email" name="email" type="email" value={value.email} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="client-phone">โทรศัพท์</label><input id="client-phone" name="phone" value={value.phone} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="client-address">ที่อยู่</label><input id="client-address" name="address" value={value.address} onChange={onChange} /></div>
      <div className="form-field">
        <label htmlFor="client-province">จังหวัด</label>
        <select id="client-province" value={value.province} onChange={handleProvinceChange} disabled={addressLoading || Boolean(addressError)}>
          <option value="">{addressLoading ? 'กำลังโหลดจังหวัด...' : 'เลือกจังหวัด'}</option>
          {provinces.map((province) => <option key={province.id} value={province.name_th}>{province.name_th}</option>)}
        </select>
      </div>
      <div className="form-field">
        <label htmlFor="client-district">อำเภอ / เขต</label>
        <select id="client-district" value={value.district} onChange={handleDistrictChange} disabled={!selectedProvince}>
          <option value="">เลือกอำเภอ / เขต</option>
          {districts.map((district) => <option key={district.id} value={district.name_th}>{district.name_th}</option>)}
        </select>
      </div>
      <div className="form-field">
        <label htmlFor="client-sub-district">ตำบล / แขวง</label>
        <select id="client-sub-district" value={value.sub_district} onChange={handleSubDistrictChange} disabled={!selectedDistrict}>
          <option value="">เลือกตำบล / แขวง</option>
          {subDistricts.map((subDistrict) => <option key={subDistrict.id} value={subDistrict.name_th}>{subDistrict.name_th}</option>)}
        </select>
      </div>
      <div className="form-field">
        <label htmlFor="client-postal-code">รหัสไปรษณีย์</label>
        <select id="client-postal-code" name="postal_code" value={value.postal_code} onChange={onChange} disabled={!selectedSubDistrict}>
          <option value="">เลือกตำบลก่อน</option>
          {selectedSubDistrict && <option value={String(selectedSubDistrict.zip_code)}>{selectedSubDistrict.zip_code}</option>}
        </select>
      </div>
      <div className="form-field"><label htmlFor="client-tax">เลขประจำตัวผู้เสียภาษี</label><input id="client-tax" name="tax_id" value={value.tax_id} onChange={onChange} /></div>
      <div className="form-field"><label htmlFor="client-status">สถานะ</label><select id="client-status" name="status" value={value.status} onChange={onChange}><option value="ACTIVE">ใช้งานอยู่</option><option value="ARCHIVED">เก็บถาวร</option></select></div>
      {addressError && <p className="form-message error">{addressError}</p>}
      <div className="form-field full"><label htmlFor="client-notes">หมายเหตุ</label><textarea id="client-notes" name="notes" value={value.notes} onChange={onChange} /></div>
    </div>
    <div className="form-actions"><button className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'กำลังบันทึก...' : 'บันทึกลูกค้า'}</button></div>
  </form>
}
