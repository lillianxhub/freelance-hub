import * as React from 'react'
import { cn } from 'cn'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './select'

const emptyValue = '__freelance_hub_empty_select_value__'

type NativeSelectProps = Omit<React.ComponentProps<'select'>, 'size'> & {
  size?: 'sm' | 'default'
  wrapperClassName?: string
  placeholder?: string
}

function isOption(
  child: React.ReactNode,
): child is React.ReactElement<React.ComponentProps<'option'>> {
  return React.isValidElement(child) && child.type === 'option'
}

/** Compatibility wrapper that renders the shadcn Select primitives. */
function NativeSelect({
  className,
  size = 'default',
  wrapperClassName,
  children,
  value,
  defaultValue,
  onChange,
  onBlur,
  disabled,
  id,
  name,
  required,
  placeholder,
  'aria-describedby': ariaDescribedBy,
  'aria-invalid': ariaInvalid,
  'aria-label': ariaLabel,
}: NativeSelectProps) {
  const options = React.Children.toArray(children).filter(isOption)
  const toInternalValue = (nextValue: string) => nextValue || emptyValue
  const fromInternalValue = (nextValue: string) =>
    nextValue === emptyValue ? '' : nextValue
  const selectedValue = value === undefined ? undefined : toInternalValue(String(value))
  const initialValue = defaultValue === undefined
    ? undefined
    : toInternalValue(String(defaultValue))

  const notifyChange = (nextValue: string) => {
    if (!onChange) return
    const fieldValue = fromInternalValue(nextValue)
    const event = {
      target: { name: name ?? '', value: fieldValue },
      currentTarget: { name: name ?? '', value: fieldValue },
    } as unknown as React.ChangeEvent<HTMLSelectElement>
    onChange(event)
  }

  return (
    <Select
      name={name}
      required={required}
      disabled={disabled}
      value={selectedValue}
      defaultValue={initialValue}
      onValueChange={notifyChange}
    >
      <SelectTrigger
        id={id}
        size={size}
        className={cn(wrapperClassName, className)}
        aria-describedby={ariaDescribedBy}
        aria-invalid={ariaInvalid}
        aria-label={ariaLabel}
        onBlur={(event) => onBlur?.(event as unknown as React.FocusEvent<HTMLSelectElement>)}
      >
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent position="popper">
        {options.map((option) => {
          const optionValue = option.props.value === undefined
            ? String(option.props.children)
            : String(option.props.value)
          return (
            <SelectItem key={option.key ?? optionValue} value={toInternalValue(optionValue)} disabled={option.props.disabled}>
              {option.props.children}
            </SelectItem>
          )
        })}
      </SelectContent>
    </Select>
  )
}

export { NativeSelect }
