import type { FormLabelProps } from '../types/ui'
import { Label } from './ui/label'

function FormLabel({ htmlFor, children, required = false }: FormLabelProps) {
  return (
    <Label htmlFor={htmlFor} className="mb-1">
      {children}
      {required && (
        <span className="text-destructive" aria-hidden="true">
          {' '}
          *
        </span>
      )}
    </Label>
  )
}

export default FormLabel
