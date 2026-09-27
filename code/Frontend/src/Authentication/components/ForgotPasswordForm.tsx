import { Link } from 'react-router-dom'
import AuthenticationLayout from './AuthenticationLayout'

export default function ForgotPasswordForm() {
  return <AuthenticationLayout>
    <div className="auth-form">
      <div className="auth-brand"><span>FH</span><strong>freelance hub</strong></div>
      <h1>ลืมรหัสผ่าน</h1>
      <p className="auth-description">ระบบตั้งรหัสผ่านใหม่ยังไม่เปิดใช้งานใน ระบบหลังบ้าน</p>
      <p className="auth-footer"><Link to="/login">← กลับไปหน้าเข้าสู่ระบบ</Link></p>
    </div>
  </AuthenticationLayout>
}
