export function isValidEmail(email: string): boolean {
  return /^\S+@\S+\.\S+$/.test(email.trim())
}

export function hasRequiredPassword(password: string): boolean {
  return password.length >= 8
}

export function passwordsMatch(password: string, confirmation: string): boolean {
  return password === confirmation
}

export function normalizePhone(phone: string): string {
  return phone.replace(/\D/g, '').slice(0, 10)
}

export function isValidPhone(phone: string): boolean {
  return /^\d{10}$/.test(phone)
}
