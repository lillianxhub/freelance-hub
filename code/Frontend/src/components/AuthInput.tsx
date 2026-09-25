import type { ChangeEventHandler, HTMLInputTypeAttribute } from 'react'

interface AuthInputProps {
  label: string
  type: HTMLInputTypeAttribute
  name: string
  value: string
  onChange: ChangeEventHandler<HTMLInputElement>
  autoComplete?: string
}

function AuthInput({ label, type, name, value, onChange, autoComplete }: AuthInputProps) {
  return (
    <>
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete || (type === 'password' ? 'current-password' : name)}
        required
      />
    </>
  )
}

export default AuthInput
