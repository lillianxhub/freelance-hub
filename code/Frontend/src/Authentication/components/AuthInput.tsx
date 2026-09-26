import type { AuthInputProps } from '../../types/auth'
import Input from '../../components/Input'

function AuthInput({ label, type, name, value, onChange, autoComplete, minLength, required = true }: AuthInputProps) {
  return (
    <>
      <label htmlFor={name}>{label}</label>
      <Input
        id={name}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete || (type === 'password' ? 'current-password' : name)}
        minLength={minLength}
        required={required}
      />
    </>
  )
}

export default AuthInput
