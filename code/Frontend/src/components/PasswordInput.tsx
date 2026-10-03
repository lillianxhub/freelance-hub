import { Button } from './ui/button'
import { useState, type ComponentProps } from 'react'
import { FiEye, FiEyeOff } from 'react-icons/fi'
import { Input } from './ui/input'
import { cn } from 'cn'

type PasswordInputProps = Omit<ComponentProps<typeof Input>, 'type'>

export default function PasswordInput(props: PasswordInputProps) {
  const [visible, setVisible] = useState(false)

  return (
    <div className="relative">
      <Input
        {...props}
        className={cn('pr-10', props.className)}
        type={visible ? 'text' : 'password'}
      />
      <Button
        variant="ghost"
        size="icon"
        className="absolute right-1 top-1/2 -translate-y-1/2 text-muted-foreground hover:bg-muted hover:text-foreground"
        type="button"
        aria-label={visible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
        title={visible ? 'ซ่อนรหัสผ่าน' : 'แสดงรหัสผ่าน'}
        onClick={() => setVisible((current) => !current)}
        disabled={props.disabled}
      >
        {visible ? <FiEyeOff aria-hidden="true" /> : <FiEye aria-hidden="true" />}
      </Button>
    </div>
  )
}
