// ใช้ mock เป็นค่าเริ่มต้นระหว่างรอ backend
// เมื่อต้องการเชื่อม API จริง ให้กำหนด VITE_USE_MOCK_API=false ในไฟล์ .env

const useMockApi = import.meta.env.VITE_USE_MOCK_API !== 'false'
const usersStorageKey = 'freelance-hub-users'

const wait = (milliseconds = 450) => new Promise((resolve) => setTimeout(resolve, milliseconds))

function getMockUsers() {
  const storedUsers = localStorage.getItem(usersStorageKey)

  if (storedUsers) {
    try {
      return JSON.parse(storedUsers)
    } catch {
      localStorage.removeItem(usersStorageKey)
    }
  }

  return [
    {
      fullName: 'Petpinyo',
      email: 'demo@freelancehub.test',
      password: 'demo1234',
    },
  ]
}

export async function loginUser(email, password) {
  if (useMockApi) {
    await wait()
    const user = getMockUsers().find(
      (item) => item.email.toLowerCase() === email.trim().toLowerCase() && item.password === password,
    )

    if (!user) {
      throw new Error('อีเมลหรือรหัสผ่านไม่ถูกต้อง')
    }

    localStorage.setItem('currentUser', JSON.stringify({ fullName: user.fullName, email: user.email }))
    return { token: `mock-token-${Date.now()}` }
  }

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
  if (useMockApi) {
    await wait()
    const users = getMockUsers()
    const normalizedEmail = email.trim().toLowerCase()

    if (users.some((user) => user.email.toLowerCase() === normalizedEmail)) {
      throw new Error('อีเมลนี้ถูกใช้งานแล้ว')
    }

    users.push({ fullName: fullName.trim(), email: normalizedEmail, password })
    localStorage.setItem(usersStorageKey, JSON.stringify(users))
    return { message: 'ลงทะเบียนสำเร็จ' }
  }

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
