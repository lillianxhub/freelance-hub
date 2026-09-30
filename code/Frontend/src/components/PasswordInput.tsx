import { Button } from './ui/button'
import { useState, type ComponentProps } from 'react'
import { FiEye, FiEyeOff } from 'react-icons/fi'
import { Input } from './ui/input'

type PasswordInputProps = Omit<ComponentProps<typeof Input>, 'type'>

export default function PasswordInput(props: PasswordInputProps) {
  const [visible, setVisible] = useState(false)

  return (
    <span className="input-control input-control-password">
      <Input {...props} type={visible ? 'text' : 'password'} />
      <Button variant="ghost"
        className="input-visibility-button"
        type="button"
        aria-label={visible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
        title={visible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
        onClick={() => setVisible((current) => !current)}
        disabled={props.disabled}
      >
        {visible ? <FiEyeOff aria-hidden="true" /> : <FiEye aria-hidden="true" />}
      </Button>
    </span>
  )
}
