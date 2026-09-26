import type { AuthInputProps } from '../../types/auth'

function AuthInput({ label, type, name, value, onChange, autoComplete, minLength }: AuthInputProps) {
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
        minLength={minLength}
        required
      />
    </>
  )
}

export default AuthInput
