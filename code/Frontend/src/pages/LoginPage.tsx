import { useState, type ChangeEvent, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthInput from '../components/AuthInput'
import { useAuth } from '../contexts/authContextValue'
import { getErrorMessage } from '../utils/errors'

function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { login, isDemoMode } = useAuth()

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await login(form.email, form.password)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err, 'เข้าสู่ระบบไม่สำเร็จ'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="auth-brand"><span>FH</span><strong>freelance hub</strong></div>
        <h1>เข้าสู่ระบบ</h1>
        <p className="auth-description">จัดการลูกค้า โปรเจกต์ เวลา และ Invoice ในที่เดียว</p>
        {error && <p className="auth-error">{error}</p>}

        <AuthInput label="อีเมล" type="email" name="email" value={form.email} onChange={handleChange} />
        <AuthInput label="รหัสผ่าน" type="password" name="password" value={form.password} onChange={handleChange} />

        <div className="auth-options"><span /> <Link to="/forgot-password">ลืมรหัสผ่าน?</Link></div>

        <button type="submit" disabled={loading}>
          {loading ? 'กำลังเข้าสู่ระบบ...' : 'เข้าสู่ระบบ'}
        </button>

        <p className="auth-footer">
          ยังไม่มีบัญชี? <Link to="/register">สมัครสมาชิก</Link>
        </p>
        {isDemoMode && <div className="demo-credentials"><strong>บัญชีทดลอง</strong><span>demo@freelancehub.test</span><span>รหัสผ่าน: demo1234</span></div>}
      </form>
    </div>
  )
}

export default LoginPage
