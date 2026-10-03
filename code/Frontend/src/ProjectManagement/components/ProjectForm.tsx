import { Input } from '../../components/ui/input'
import { NativeSelect } from '../../components/ui/native-select'
import { Textarea } from '../../components/ui/textarea'
import { Button } from '../../components/ui/button'
import { DatePicker } from '../../components/ui/date-picker'
import type { ChangeEvent } from 'react'
import type { ProjectFormProps } from "../../types/projectsPage";
import FormLabel from "../../components/FormLabel";

export default function ProjectForm({
  value,
  clients,
  error,
  dateErrors,
  saving,
  onChange,
  onSubmit,
  onCancel,
}: ProjectFormProps) {
  const handleDateChange = (name: 'start_date' | 'end_date', value: string) => {
    onChange({ target: { name, value }, currentTarget: { name, value } } as ChangeEvent<HTMLInputElement>)
  }

  return (
    <form onSubmit={onSubmit}>
      {error && <p className="mb-4 rounded-lg bg-red-soft px-3 py-2 text-sm text-destructive">{error}</p>}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1.5">
          <FormLabel htmlFor="project-name" required>ชื่อโปรเจกต์</FormLabel>
          <Input
            id="project-name"
            name="name"
            value={value.name}
            onChange={onChange}
            required
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <FormLabel htmlFor="project-client" required>ลูกค้า</FormLabel>
          <NativeSelect
            id="project-client"
            name="client_id"
            value={value.client_id}
            onChange={onChange}
            required
          >
            {clients
              .filter(
                (client) =>
                  client.status === "ACTIVE" || client.id === value.client_id,
              )
              .map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
          </NativeSelect>
        </div>
        {/* <div className="flex flex-col gap-1.5">
          <label htmlFor="project-billing">รูปแบบราคา</label>
          <select
            id="project-billing"
            name="billing_type"
            value={value.billing_type}
            onChange={onChange}
          >
            <option value="HOURLY">รายชั่วโมง</option>
            <option value="FIXED_PRICE">เหมาจ่าย</option>
          </select>
        </div> */}
        {/* {value.billing_type === "HOURLY" ? (
          <div className="flex flex-col gap-1.5">
            <label htmlFor="hourly-rate">อัตราต่อชั่วโมง</label>
            <input
              id="hourly-rate"
              name="hourly_rate"
              type="number"
              min="0"
              step="0.01"
              value={value.hourly_rate}
              onChange={onChange}
              required
            />
          </div>
        ) : (
          <div className="flex flex-col gap-1.5">
            <label htmlFor="fixed-price">มูลค่างาน</label>
            <input
              id="fixed-price"
              name="fixed_price"
              type="number"
              min="0"
              step="0.01"
              value={value.fixed_price}
              onChange={onChange}
              required
            />
          </div>
        )} */}
        <div className="flex flex-col gap-1.5">
          <FormLabel htmlFor="budget-hours" required>จำนวนชั่วโมง</FormLabel>
          <Input
            id="budget-hours"
            name="budget_hours"
            type="number"
            min="0"
            step="0.5"
            value={value.budget_hours}
            onChange={onChange}
            required
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <FormLabel htmlFor="start-date" required>วันที่เริ่ม</FormLabel>
          <DatePicker
            id="start-date"
            value={value.start_date || ""}
            onChange={(nextValue) => handleDateChange('start_date', nextValue)}
            required
            aria-label="วันที่เริ่ม"
            aria-invalid={Boolean(dateErrors.start_date)}
            aria-describedby={dateErrors.start_date ? 'start-date-error' : undefined}
          />
          {dateErrors.start_date && <p id="start-date-error" className="text-sm text-destructive" role="alert">{dateErrors.start_date}</p>}
        </div>
        <div className="flex flex-col gap-1.5">
          <FormLabel htmlFor="end-date" required>วันที่สิ้นสุด</FormLabel>
          <DatePicker
            id="end-date"
            value={value.end_date || ""}
            onChange={(nextValue) => handleDateChange('end_date', nextValue)}
            required
            aria-label="วันที่สิ้นสุด"
            aria-invalid={Boolean(dateErrors.end_date)}
            aria-describedby={dateErrors.end_date ? 'end-date-error' : undefined}
          />
          {dateErrors.end_date && <p id="end-date-error" className="text-sm text-destructive" role="alert">{dateErrors.end_date}</p>}
        </div>
        <div className="flex flex-col gap-1.5">
          <FormLabel htmlFor="project-color" required>สีโปรเจกต์</FormLabel>
          <Input
            id="project-color"
            name="color"
            type="color"
            value={value.color}
            onChange={onChange}
            required
          />
        </div>
        {/* <div className="flex flex-col gap-1.5">
          <label htmlFor="project-currency">สกุลเงิน</label>
          <select
            id="project-currency"
            name="currency"
            value={value.currency}
            onChange={onChange}
          >
            <option value="THB">THB</option>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
          </select>
        </div> */}
        <div className="col-span-full flex flex-col gap-1.5">
          <FormLabel htmlFor="project-description">รายละเอียด</FormLabel>
          <Textarea
            id="project-description"
            name="description"
            value={value.description}
            onChange={onChange}
          />
        </div>
      </div>
      <div className="mt-4 flex justify-end gap-2 border-t border-border pt-4">
        <Button variant="outline"
          className="h-10"
          type="button"
          onClick={onCancel}
        >
          ยกเลิก
        </Button>
        <Button variant="default"
          className="h-10"
          type="submit"
          disabled={saving}
        >
          {saving ? "กำลังบันทึก..." : "บันทึกโปรเจกต์"}
        </Button>
      </div>
    </form>
  );
}
