import { Input } from '../../components/ui/input'
import { NativeSelect } from '../../components/ui/native-select'
import { Textarea } from '../../components/ui/textarea'
import { Button } from '../../components/ui/button'
import type { ProjectFormProps } from "../../types/projectsPage";
import FormLabel from "../../components/FormLabel";

export default function ProjectForm({
  value,
  clients,
  error,
  saving,
  onChange,
  onSubmit,
  onCancel,
}: ProjectFormProps) {
  return (
    <form onSubmit={onSubmit}>
      {error && <p className="form-error">{error}</p>}
      <div className="form-grid">
        <div className="form-field">
          <FormLabel htmlFor="project-name" required>ชื่อโปรเจกต์</FormLabel>
          <Input
            id="project-name"
            name="name"
            value={value.name}
            onChange={onChange}
            required
          />
        </div>
        <div className="form-field">
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
                  {client.company_name || client.name}
                </option>
              ))}
          </NativeSelect>
        </div>
        {/* <div className="form-field">
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
          <div className="form-field">
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
          <div className="form-field">
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
        <div className="form-field">
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
        <div className="form-field">
          <FormLabel htmlFor="start-date" required>วันที่เริ่ม</FormLabel>
          <Input
            id="start-date"
            name="start_date"
            type="date"
            value={value.start_date || ""}
            onChange={onChange}
            required
          />
        </div>
        <div className="form-field">
          <FormLabel htmlFor="end-date" required>วันที่สิ้นสุด</FormLabel>
          <Input
            id="end-date"
            name="end_date"
            type="date"
            value={value.end_date || ""}
            onChange={onChange}
            required
          />
        </div>
        <div className="form-field">
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
        {/* <div className="form-field">
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
        <div className="form-field full">
          <FormLabel htmlFor="project-description">รายละเอียด</FormLabel>
          <Textarea
            id="project-description"
            name="description"
            value={value.description}
            onChange={onChange}
          />
        </div>
      </div>
      <div className="form-actions">
        <Button variant="outline"
          className="button button-secondary"
          type="button"
          onClick={onCancel}
        >
          ยกเลิก
        </Button>
        <Button variant="default"
          className="button button-primary"
          type="submit"
          disabled={saving}
        >
          {saving ? "กำลังบันทึก..." : "บันทึกโปรเจกต์"}
        </Button>
      </div>
    </form>
  );
}
