import AuthenticationLayout from './AuthenticationLayout'
import { useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthInput from './AuthInput'
import { useAuth } from '../useAuthentication'
import { getErrorMessage } from '../../api/apiError'

function RegisterForm() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
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
    setLoading(true)

    try {
      await register(form.fullName, form.email, form.password)
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

        <AuthInput label="ชื่อ-นามสกุล" type="text" name="fullName" value={form.fullName} onChange={handleChange} />
        <AuthInput label="อีเมล" type="email" name="email" value={form.email} onChange={handleChange} />
        <AuthInput label="รหัสผ่าน" type="password" name="password" value={form.password} onChange={handleChange} autoComplete="new-password" minLength={8} />

        <button className="button button-primary wide" type="submit" disabled={loading}>
          {loading ? 'กำลังสมัคร...' : 'สมัครสมาชิก'}
        </button>

        <p className="auth-footer">
          มีบัญชีอยู่แล้ว? <Link to="/login">เข้าสู่ระบบ</Link>
        </p>
      </form>
    </AuthenticationLayout>
  )
}

export default RegisterForm
