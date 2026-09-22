// จำลองการเรียก API login/register
// พอ backend เสร็จ เปลี่ยนแค่ไฟล์นี้ ไม่ต้องแก้ LoginPage/RegisterPage

export async function loginUser(email, password) {
  const response = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })

  if (!response.ok) {
    throw new Error('อีเมลหรือรหัสผ่านไม่ถูกต้อง')
  }

  return response.json() // { token: "..." }
}

export async function registerUser(fullName, email, password) {
  const response = await fetch('http://localhost:8080/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, email, password }),
  })

  if (!response.ok) {
    throw new Error('สมัครสมาชิกไม่สำเร็จ')
  }

  return response.json()
}