import type { AuthInputProps } from '../../types/auth'
import Input from '../../components/Input'
import FormLabel from '../../components/FormLabel'

function AuthInput({
  label,
  type,
  name,
  value,
  onChange,
  autoComplete,
  error,
  required = true,
  ...inputProps
}: AuthInputProps) {
  const errorId = `${name}-error`

  return (
    <div className="auth-field">
      <FormLabel htmlFor={name} required={required}>{label}</FormLabel>
      <Input
        id={name}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete || (type === 'password' ? 'current-password' : name)}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
        {...inputProps}
      />
      {error && <p id={errorId} className="field-error">{error}</p>}
    </div>
  )
}

export default AuthInput
