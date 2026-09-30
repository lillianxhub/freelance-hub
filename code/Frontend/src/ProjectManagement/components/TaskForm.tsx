import { Input } from '../../components/ui/input'
import { Textarea } from '../../components/ui/textarea'
import { Button } from '../../components/ui/button'
import FormLabel from "../../components/FormLabel";
import type { TaskFormProps } from "../../types/projectDetailPage";

export default function TaskForm({
  value,
  error,
  onChange,
  onSubmit,
  onCancel,
}: TaskFormProps) {
  return (
    <form onSubmit={onSubmit}>
      {error && <p className="form-error">{error}</p>}
      <div className="form-grid">
        <div className="form-field full">
          <FormLabel htmlFor="task-name" required>ชื่องาน</FormLabel>
          <Input
            id="task-name"
            value={value.name}
            onChange={(event) =>
              onChange({ ...value, name: event.target.value })
            }
            required
          />
        </div>
        {/* <div className="form-field">
          <label htmlFor="task-status">สถานะ</label>
          <select
            id="task-status"
            value={value.status}
            onChange={(event) =>
              onChange({ ...value, status: event.target.value as TaskStatus })
            }
          >
            <option value="TODO">รอดำเนินการ</option>
            <option value="IN_PROGRESS">กำลังทำ</option>
            <option value="IN_REVIEW">ตรวจสอบ</option>
            <option value="DONE">เสร็จแล้ว</option>
          </select>
        </div> */}
        {/* <div className="form-field">
          <label htmlFor="task-due">กำหนดส่ง</label>
          <input
            id="task-due"
            type="date"
            value={value.due_date || ""}
            onChange={(event) =>
              onChange({ ...value, due_date: event.target.value })
            }
          />
        </div> */}
        <div className="form-field full">
          <FormLabel htmlFor="task-description">รายละเอียด</FormLabel>
          <Textarea
            id="task-description"
            value={value.description}
            onChange={(event) =>
              onChange({ ...value, description: event.target.value })
            }
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
        <Button variant="default" className="button button-primary" type="submit">
          บันทึก งาน
        </Button>
      </div>
    </form>
  );
}
