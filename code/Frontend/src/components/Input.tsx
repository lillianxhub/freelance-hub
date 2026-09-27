import { useState, type InputHTMLAttributes } from 'react'
import { FiEye, FiEyeOff } from 'react-icons/fi'

type InputProps = InputHTMLAttributes<HTMLInputElement>

function Input({ className, type = 'text', ...props }: InputProps) {
  const [passwordVisible, setPasswordVisible] = useState(false)
  const isPassword = type === 'password'
  const inputType = isPassword && passwordVisible ? 'text' : type

  return (
    <span className={`input-control${isPassword ? ' input-control-password' : ''}`}>
      <input {...props} className={className} type={inputType} />
      {isPassword && (
        <button
          className="input-visibility-button"
          type="button"
          aria-label={passwordVisible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
          title={passwordVisible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
          onClick={() => setPasswordVisible((visible) => !visible)}
        >
          {passwordVisible ? <FiEyeOff aria-hidden="true" /> : <FiEye aria-hidden="true" />}
        </button>
      )}
    </span>
  )
}

export default Input
