import type { FormLabelProps } from '../types/ui'
import { Label } from './ui/label'

function FormLabel({ htmlFor, children, required = false }: FormLabelProps) {
  return (
    <Label htmlFor={htmlFor}>
      {children}
      {required && <span className="required-mark" aria-hidden="true"> *</span>}
    </Label>
  )
}

export default FormLabel
