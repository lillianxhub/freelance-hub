import { useState } from 'react'
import { Link } from 'react-router-dom'
import AuthInput from '../components/AuthInput'
import { sendPasswordReset } from '../services/authService'

function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setMessage('')
    try {
      await sendPasswordReset(email)
      setMessage('ส่งลิงก์ตั้งรหัสผ่านใหม่แล้ว กรุณาตรวจสอบอีเมล')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <div className="auth-brand"><span>FH</span><strong>freelance hub</strong></div>
        <h1>ลืมรหัสผ่าน</h1>
        <p className="auth-description">กรอกอีเมลที่ใช้สมัคร เราจะส่งลิงก์สำหรับตั้งรหัสผ่านใหม่</p>
        {error && <p className="form-message error">{error}</p>}
        {message && <p className="form-message success">{message}</p>}
        <AuthInput label="อีเมล" type="email" name="email" value={email} onChange={(event) => setEmail(event.target.value)} />
        <button className="button button-primary wide" type="submit" disabled={loading}>{loading ? 'กำลังส่ง...' : 'ส่งลิงก์'}</button>
        <p className="auth-footer"><Link to="/login">← กลับไปหน้าเข้าสู่ระบบ</Link></p>
      </form>
    </div>
  )
}

export default ForgotPasswordPage
