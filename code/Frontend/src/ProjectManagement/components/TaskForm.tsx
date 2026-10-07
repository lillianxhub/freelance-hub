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
      {error && <p className="mb-4 rounded-lg bg-red-soft px-3 py-2 text-sm text-destructive">{error}</p>}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="col-span-full flex flex-col gap-1.5">
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
        <div className="col-span-full flex flex-col gap-1.5">
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
      <div className="mt-4 flex justify-end gap-2 border-t border-border pt-4">
        <Button variant="outline"
          className="h-10"
          type="button"
          onClick={onCancel}
        >
          ยกเลิก
        </Button>
        <Button variant="default" className="h-10" type="submit">
          บันทึก งาน
        </Button>
      </div>
    </form>
  );
}
