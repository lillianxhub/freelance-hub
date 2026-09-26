import AuthenticationLayout from './AuthenticationLayout'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthInput from './AuthInput'
import Button from '../../components/Button'
import { useAuth } from '../useAuthentication'
import { getErrorMessage } from '../../api/apiError'
import { hasRequiredPassword, passwordsMatch } from '../authentication.validators'
import type { RegisterFormValues } from '../../types/auth'

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
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { register } = useAuth()

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError('')

    if (!hasRequiredPassword(form.password)) {
      setError('รหัสผ่านต้องมีอย่างน้อย 8 ตัวอักษร')
      return
    }

    if (!passwordsMatch(form.password, form.confirmPassword)) {
      setError('รหัสผ่านและการยืนยันรหัสผ่านไม่ตรงกัน')
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

        <AuthInput label="ชื่อที่แสดง" type="text" name="displayName" value={form.displayName} onChange={handleChange} autoComplete="nickname" />
        <AuthInput label="ชื่อ" type="text" name="firstName" value={form.firstName} onChange={handleChange} autoComplete="given-name" />
        <AuthInput label="นามสกุล" type="text" name="lastName" value={form.lastName} onChange={handleChange} autoComplete="family-name" />
        <AuthInput label="อีเมล" type="email" name="email" value={form.email} onChange={handleChange} />
        <AuthInput label="เบอร์โทรศัพท์" type="tel" name="phone" value={form.phone} onChange={handleChange} autoComplete="tel" required={false} />
        <AuthInput label="รหัสผ่าน" type="password" name="password" value={form.password} onChange={handleChange} autoComplete="new-password" minLength={8} />
        <AuthInput label="ยืนยันรหัสผ่าน" type="password" name="confirmPassword" value={form.confirmPassword} onChange={handleChange} autoComplete="new-password" minLength={8} />

        <Button className="wide" type="submit" disabled={loading}>
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
