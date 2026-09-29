import { useEffect, useState, type ChangeEvent, type FocusEvent, type FormEvent } from 'react'
import FormLabel from '../../components/FormLabel'
import type { ClientFieldErrors, ClientFieldName, ClientFormProps } from '../../types/clientsPage'
import type { FieldErrorProps } from '../../types/ui'
import type { ResourceInput } from '../../types/workspace'
import { loadThaiAddressData, type ThaiProvince } from '../../services/thaiAddress'
import { validateClientField, validateClientFields } from '../client.validators'

function FieldError({ id, message }: FieldErrorProps) {
  return message ? <p id={id} className="field-error">{message}</p> : null
}

function ClientForm({ value, error, saving, onChange, onFieldsChange, onSubmit, onCancel }: ClientFormProps) {
  const [provinces, setProvinces] = useState<ThaiProvince[]>([])
  const [addressLoading, setAddressLoading] = useState(true)
  const [addressError, setAddressError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<ClientFieldErrors>({})

  useEffect(() => {
    let active = true
    loadThaiAddressData()
      .then((items) => { if (active) setProvinces(items) })
      .catch(() => { if (active) setAddressError('ไม่สามารถโหลดข้อมูลจังหวัดได้') })
      .finally(() => { if (active) setAddressLoading(false) })
    return () => { active = false }
  }, [])

  const selectedProvince = provinces.find((item) => item.name_th === value.province)
  const districts = selectedProvince?.districts || []
  const selectedDistrict = districts.find((item) => item.name_th === value.district)
  const subDistricts = selectedDistrict?.sub_districts || []
  const selectedSubDistrict = subDistricts.find((item) => item.name_th === value.sub_district)

  const updateFieldError = (field: ClientFieldName, nextValue: ResourceInput<'clients'>) => {
    if (!fieldErrors[field]) return
    setFieldErrors((current) => ({ ...current, [field]: validateClientField(field, nextValue) }))
  }

  const handleInputChange = (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const field = event.target.name as ClientFieldName
    const nextFieldValue = field === 'phone'
      ? event.target.value.replace(/\D/g, '').slice(0, 10)
      : field === 'tax_id'
        ? event.target.value.replace(/\D/g, '').slice(0, 13)
        : event.target.value
    event.target.value = nextFieldValue
    const nextValue = { ...value, [field]: nextFieldValue }
    onChange(event)
    updateFieldError(field, nextValue)
  }

  const handleBlur = (event: FocusEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const field = event.target.name as ClientFieldName
    setFieldErrors((current) => ({ ...current, [field]: validateClientField(field, value) }))
  }

  const handleProvinceChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextValue = { ...value, province: event.target.value, district: '', sub_district: '', postal_code: '' }
    onFieldsChange({ province: event.target.value, district: '', sub_district: '', postal_code: '' })
    updateFieldError('province', nextValue)
  }

  const handleDistrictChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const nextValue = { ...value, district: event.target.value, sub_district: '', postal_code: '' }
    onFieldsChange({ district: event.target.value, sub_district: '', postal_code: '' })
    updateFieldError('district', nextValue)
  }

  const handleSubDistrictChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const subDistrict = subDistricts.find((item) => item.name_th === event.target.value)
    const postalCode = subDistrict ? String(subDistrict.zip_code) : ''
    const nextValue = { ...value, sub_district: event.target.value, postal_code: postalCode }
    onFieldsChange({ sub_district: event.target.value, postal_code: postalCode })
    updateFieldError('sub_district', nextValue)
    updateFieldError('postal_code', nextValue)
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    const validationErrors = validateClientFields(value)
    setFieldErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      event.preventDefault()
      return
    }
    onSubmit(event)
  }

  return <form onSubmit={handleSubmit} noValidate>
    {error && <p className="form-error">{error}</p>}
    <div className="form-grid">
      <div className="form-field"><FormLabel htmlFor="client-name" required>ชื่อผู้ติดต่อ</FormLabel><input id="client-name" name="name" value={value.name} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.name)} aria-describedby={fieldErrors.name ? 'client-name-error' : undefined} placeholder="ชื่อ-นามสกุล" /><FieldError id="client-name-error" message={fieldErrors.name} /></div>
      <div className="form-field"><FormLabel htmlFor="company-name" required>ชื่อบริษัท</FormLabel><input id="company-name" name="company_name" value={value.company_name} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.company_name)} aria-describedby={fieldErrors.company_name ? 'company-name-error' : undefined} placeholder="บริษัท จำกัด" /><FieldError id="company-name-error" message={fieldErrors.company_name} /></div>
      <div className="form-field"><FormLabel htmlFor="client-email" required>อีเมล</FormLabel><input id="client-email" name="email" type="email" value={value.email} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.email)} aria-describedby={fieldErrors.email ? 'client-email-error' : undefined} autoComplete="email" /><FieldError id="client-email-error" message={fieldErrors.email} /></div>
      <div className="form-field"><FormLabel htmlFor="client-phone" required>โทรศัพท์</FormLabel><input id="client-phone" name="phone" type="tel" value={value.phone} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.phone)} aria-describedby={fieldErrors.phone ? 'client-phone-error' : undefined} autoComplete="tel" inputMode="numeric" maxLength={10} pattern="[0-9]{10}" /><FieldError id="client-phone-error" message={fieldErrors.phone} /></div>
      <div className="form-field"><FormLabel htmlFor="client-address" required>ที่อยู่</FormLabel><input id="client-address" name="address" value={value.address} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.address)} aria-describedby={fieldErrors.address ? 'client-address-error' : undefined} /><FieldError id="client-address-error" message={fieldErrors.address} /></div>
      <div className="form-field"><FormLabel htmlFor="client-province" required>จังหวัด</FormLabel><select id="client-province" name="province" value={value.province} onChange={handleProvinceChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.province)} aria-describedby={fieldErrors.province ? 'client-province-error' : undefined} disabled={addressLoading || Boolean(addressError)}><option value="">{addressLoading ? 'กำลังโหลดจังหวัด...' : 'เลือกจังหวัด'}</option>{provinces.map((province) => <option key={province.id} value={province.name_th}>{province.name_th}</option>)}</select><FieldError id="client-province-error" message={fieldErrors.province} /></div>
      <div className="form-field"><FormLabel htmlFor="client-district" required>อำเภอ / เขต</FormLabel><select id="client-district" name="district" value={value.district} onChange={handleDistrictChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.district)} aria-describedby={fieldErrors.district ? 'client-district-error' : undefined} disabled={!selectedProvince}><option value="">เลือกอำเภอ / เขต</option>{districts.map((district) => <option key={district.id} value={district.name_th}>{district.name_th}</option>)}</select><FieldError id="client-district-error" message={fieldErrors.district} /></div>
      <div className="form-field"><FormLabel htmlFor="client-sub-district" required>ตำบล / แขวง</FormLabel><select id="client-sub-district" name="sub_district" value={value.sub_district} onChange={handleSubDistrictChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.sub_district)} aria-describedby={fieldErrors.sub_district ? 'client-sub-district-error' : undefined} disabled={!selectedDistrict}><option value="">เลือกตำบล / แขวง</option>{subDistricts.map((subDistrict) => <option key={subDistrict.id} value={subDistrict.name_th}>{subDistrict.name_th}</option>)}</select><FieldError id="client-sub-district-error" message={fieldErrors.sub_district} /></div>
      <div className="form-field"><FormLabel htmlFor="client-postal-code" required>รหัสไปรษณีย์</FormLabel><select id="client-postal-code" name="postal_code" value={value.postal_code} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.postal_code)} aria-describedby={fieldErrors.postal_code ? 'client-postal-code-error' : undefined} disabled={!selectedSubDistrict}><option value="">เลือกตำบลก่อน</option>{selectedSubDistrict && <option value={String(selectedSubDistrict.zip_code)}>{selectedSubDistrict.zip_code}</option>}</select><FieldError id="client-postal-code-error" message={fieldErrors.postal_code} /></div>
      <div className="form-field"><FormLabel htmlFor="client-tax" required>เลขประจำตัวผู้เสียภาษี</FormLabel><input id="client-tax" name="tax_id" value={value.tax_id} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.tax_id)} aria-describedby={fieldErrors.tax_id ? 'client-tax-error' : undefined} inputMode="numeric" maxLength={13} pattern="[0-9]{13}" /><FieldError id="client-tax-error" message={fieldErrors.tax_id} /></div>
      <div className="form-field"><FormLabel htmlFor="client-status" required>สถานะ</FormLabel><select id="client-status" name="status" value={value.status} onChange={handleInputChange} onBlur={handleBlur} aria-invalid={Boolean(fieldErrors.status)} aria-describedby={fieldErrors.status ? 'client-status-error' : undefined}><option value="ACTIVE">ใช้งานอยู่</option><option value="ARCHIVED">เก็บถาวร</option></select><FieldError id="client-status-error" message={fieldErrors.status} /></div>
      {addressError && <p className="form-message error">{addressError}</p>}
      <div className="form-field full"><FormLabel htmlFor="client-notes">หมายเหตุ</FormLabel><textarea id="client-notes" name="notes" value={value.notes} onChange={handleInputChange} /></div>
    </div>
    <div className="form-actions"><button className="button button-secondary" type="button" onClick={onCancel}>ยกเลิก</button><button className="button button-primary" type="submit" disabled={saving}>{saving ? 'กำลังบันทึก...' : 'บันทึกลูกค้า'}</button></div>
  </form>
}

export default ClientForm
