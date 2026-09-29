import type { FormLabelProps } from '../types/ui'

function FormLabel({ htmlFor, children, required = false }: FormLabelProps) {
  return (
    <label htmlFor={htmlFor}>
      {children}
      {required && <span className="required-mark" aria-hidden="true"> *</span>}
    </label>
  )
}

export default FormLabel
