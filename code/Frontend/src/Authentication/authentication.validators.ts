export function isValidEmail(email: string): boolean {
  return /^\S+@\S+\.\S+$/.test(email.trim())
}

export function hasRequiredPassword(password: string): boolean {
  return password.length >= 8
}

export function passwordsMatch(password: string, confirmation: string): boolean {
  return password === confirmation
}
