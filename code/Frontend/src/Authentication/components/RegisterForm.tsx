import AuthenticationLayout from './AuthenticationLayout'
import { useState, type ChangeEvent, type FocusEvent, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthInput from './AuthInput'
import { Button } from '../../components/ui/button'
import { useAuth } from '../useAuthentication'
import { getErrorMessage } from '../../api/apiError'
import { hasRequiredPassword, isValidEmail, isValidPhone, normalizePhone, passwordsMatch } from '../authentication.validators'
import type { RegisterFieldErrors, RegisterFormValues } from '../../types/auth'

function RegisterForm() {
  const [form, setForm] = useState<RegisterFormValues>({
    displayName: '',
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  })
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<RegisterFieldErrors>({})
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { register } = useAuth()

  const validateField = (
    name: keyof RegisterFormValues,
    values: RegisterFormValues,
  ): string | undefined => {
    const value = values[name].trim()

    if (!value) return 'กรุณากรอกข้อมูลในช่องนี้'
    if (name === 'email' && !isValidEmail(values.email)) return 'กรุณากรอกอีเมลให้ถูกต้อง'
    if (name === 'phone' && !isValidPhone(values.phone)) return 'กรุณากรอกเบอร์โทรศัพท์ 10 หลัก'
    if (name === 'password' && !hasRequiredPassword(values.password)) return 'รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร'
    if (name === 'confirmPassword' && !passwordsMatch(values.password, values.confirmPassword)) {
      return 'รหัสผ่านและการยืนยันรหัสผ่านไม่ตรงกัน'
    }

    return undefined
  }

  const validateForm = (values: RegisterFormValues): RegisterFieldErrors => {
    const names: Array<keyof RegisterFormValues> = [
      'displayName',
      'firstName',
      'lastName',
      'email',
      'phone',
      'password',
      'confirmPassword',
    ]

    return names.reduce<RegisterFieldErrors>((errors, name) => {
      const fieldError = validateField(name, values)
      if (fieldError) errors[name] = fieldError
      return errors
    }, {})
  }

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const name = e.target.name as keyof RegisterFormValues
    const value = name === 'phone' ? normalizePhone(e.target.value) : e.target.value
    const nextForm = { ...form, [name]: value }
    setForm(nextForm)

    if (fieldErrors[name]) {
      setFieldErrors((current) => ({
        ...current,
        [name]: validateField(name, nextForm),
      }))
    }

    if ((name === 'password' || name === 'confirmPassword') && fieldErrors.confirmPassword) {
      setFieldErrors((current) => ({
        ...current,
        confirmPassword: validateField('confirmPassword', nextForm),
      }))
    }
  }

  const handleBlur = (e: FocusEvent<HTMLInputElement>) => {
    const name = e.target.name as keyof RegisterFormValues
    setFieldErrors((current) => ({
      ...current,
      [name]: validateField(name, form),
    }))
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError('')

    const validationErrors = validateForm(form)
    setFieldErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) {
      return
    }

    setLoading(true)

    try {
      await register({
        displayName: form.displayName,
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        phone: form.phone,
        password: form.password,
      })
      navigate('/login', { replace: true, state: { message: 'สมัครสมาชิกสำเร็จ กรุณาเข้าสู่ระบบ' } })
    } catch (err) {
      setError(getErrorMessage(err, 'สมัครสมาชิกไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthenticationLayout>
      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="auth-brand"><span>FH</span><strong>freelance hub</strong></div>
        <h1>สมัครสมาชิก</h1>
        <p className="auth-description">สร้าง พื้นที่ทำงานสำหรับจัดการงานฟรีแลนซ์ของคุณ</p>
        {error && <p className="auth-error">{error}</p>}

        <AuthInput label="ชื่อที่แสดง" type="text" name="displayName" value={form.displayName} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.displayName} autoComplete="nickname" />
        <AuthInput label="ชื่อ" type="text" name="firstName" value={form.firstName} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.firstName} autoComplete="given-name" />
        <AuthInput label="นามสกุล" type="text" name="lastName" value={form.lastName} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.lastName} autoComplete="family-name" />
        <AuthInput label="อีเมล" type="email" name="email" value={form.email} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.email} autoComplete="email" />
        <AuthInput label="เบอร์โทรศัพท์" type="tel" name="phone" value={form.phone} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.phone} autoComplete="tel" inputMode="numeric" pattern="[0-9]{10}" maxLength={10} />
        <AuthInput label="รหัสผ่าน" type="password" name="password" value={form.password} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.password} autoComplete="new-password" minLength={8} />
        <AuthInput label="ยืนยันรหัสผ่าน" type="password" name="confirmPassword" value={form.confirmPassword} onChange={handleChange} onBlur={handleBlur} error={fieldErrors.confirmPassword} autoComplete="new-password" minLength={8} />

        <Button className="wide min-h-10" type="submit" disabled={loading}>
          {loading ? 'กำลังสมัคร...' : 'สมัครสมาชิก'}
        </Button>

        <p className="auth-footer">
          มีบัญชีอยู่แล้ว? <Link to="/login">เข้าสู่ระบบ</Link>
        </p>
      </form>
    </AuthenticationLayout>
  )
}

export default RegisterForm
