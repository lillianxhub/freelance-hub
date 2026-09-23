import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthInput from '../components/AuthInput'
import { registerUser } from '../data/mockAuth'
import '../App.css'

function RegisterPage() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      await registerUser(form.fullName, form.email, form.password)
      alert('สมัครสมาชิกสำเร็จ กรุณาเข้าสู่ระบบ')
      navigate('/login')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>สมัครสมาชิก</h1>
        {error && <p className="auth-error">{error}</p>}

        <AuthInput label="ชื่อ-นามสกุล" type="text" name="fullName" value={form.fullName} onChange={handleChange} />
        <AuthInput label="อีเมล" type="email" name="email" value={form.email} onChange={handleChange} />
        <AuthInput label="รหัสผ่าน" type="password" name="password" value={form.password} onChange={handleChange} />

        <button type="submit" disabled={loading}>
          {loading ? 'กำลังสมัคร...' : 'สมัครสมาชิก'}
        </button>

        <p className="auth-footer">
          มีบัญชีอยู่แล้ว? <Link to="/login">เข้าสู่ระบบ</Link>
        </p>
      </form>
    </div>
  )
}

export default RegisterPage