const THAI_ADDRESS_API = 'https://raw.githubusercontent.com/kongvut/thai-province-data/refs/heads/master/api/latest/province_with_district_and_sub_district.json'

export interface ThaiSubDistrict {
  id: number
  name_th: string
  zip_code: number
}

export interface ThaiDistrict {
  id: number
  name_th: string
  sub_districts: ThaiSubDistrict[]
}

export interface ThaiProvince {
  id: number
  name_th: string
  districts: ThaiDistrict[]
}

let addressDataPromise: Promise<ThaiProvince[]> | null = null

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value)
}

function isString(value: unknown): value is string {
  return typeof value === 'string'
}

function parseSubDistrict(value: unknown): ThaiSubDistrict | null {
  if (!isRecord(value) || !isNumber(value.id) || !isString(value.name_th) || !isNumber(value.zip_code)) return null
  return { id: value.id, name_th: value.name_th, zip_code: value.zip_code }
}

function parseDistrict(value: unknown): ThaiDistrict | null {
  if (!isRecord(value) || !isNumber(value.id) || !isString(value.name_th) || !Array.isArray(value.sub_districts)) return null
  const subDistricts = value.sub_districts.map(parseSubDistrict).filter((item): item is ThaiSubDistrict => item !== null)
  return { id: value.id, name_th: value.name_th, sub_districts: subDistricts }
}

function parseProvince(value: unknown): ThaiProvince | null {
  if (!isRecord(value) || !isNumber(value.id) || !isString(value.name_th) || !Array.isArray(value.districts)) return null
  const districts = value.districts.map(parseDistrict).filter((item): item is ThaiDistrict => item !== null)
  return { id: value.id, name_th: value.name_th, districts }
}

function parseAddressData(value: unknown): ThaiProvince[] {
  if (!Array.isArray(value)) throw new Error('รูปแบบข้อมูลที่อยู่ไม่ถูกต้อง')
  return value.map(parseProvince).filter((item): item is ThaiProvince => item !== null)
}

export function loadThaiAddressData(): Promise<ThaiProvince[]> {
  if (!addressDataPromise) {
    addressDataPromise = fetch(THAI_ADDRESS_API)
      .then(async (response) => {
        if (!response.ok) throw new Error('ไม่สามารถโหลดข้อมูลที่อยู่ได้')
        return parseAddressData(await response.json() as unknown)
      })
      .catch((error: unknown) => {
        addressDataPromise = null
        throw error
      })
  }
  return addressDataPromise
}
