import { Button } from '../../../components/ui/button'
import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { FiPlus } from "react-icons/fi";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../components/ui/dialog";
import PageHeader from "../../../components/PageHeader";
import {
  ErrorState,
  LoadingState,
} from "../../../components/ViewState";
import { useTimeEntries } from "../../useTimeEntries";
import type { TimeEntry } from "../../../types/timeTracking";
import type { RangePreset, TimeFilters } from "../../../types/timeTrackerPage";
import TimerPanel from "../../components/TimerPanel";
import TimeEntriesCard from "../../components/TimeEntriesCard";
import TimeEntryForm from "../../components/TimeEntryForm";
import {
  createEmptyManualForm,
  localDateValue,
} from "../../../lib/timeTracking";
import { listTimerTasks } from "../../../services/timerOptions";
import {
  createManualTimeEntry,
  listTimeEntriesPage,
  summarizeTimeEntries,
  updateTimeEntry,
} from "../../../services/timeTracking";
import { getErrorMessage } from "../../../api/apiError";
import type { Task } from "../../../types/task";

const TIME_ENTRY_PAGE_LIMIT = 5;

function toApiDateBoundary(value: string, endExclusive = false): string | undefined {
  if (!value) return undefined;
  const date = new Date(`${value}T00:00:00+07:00`);
  if (endExclusive) date.setUTCDate(date.getUTCDate() + 1);
  return date.toISOString();
}

function TimeTrackerPage() {
  const workspace = useTimeEntries();
  const { data, loading, error, refresh, saveTimeEntry, deleteTimeEntry } = workspace;
  const [manualOpen, setManualOpen] = useState(false);
  const [manualForm, setManualForm] = useState(createEmptyManualForm());
  const [manualTasks, setManualTasks] = useState<Task[]>([]);
  const [manualTasksLoaded, setManualTasksLoaded] = useState(false);
  const [filterTasks, setFilterTasks] = useState<Task[]>([]);
  const [formError, setFormError] = useState("");
  const [filters, setFilters] = useState<TimeFilters>({
    client: "ALL",
    project: "ALL",
    task: "ALL",
    billable: "ALL",
    from: "",
    to: "",
  });
  const [timeEntryPage, setTimeEntryPage] = useState(1);
  const [rangePreset, setRangePreset] = useState<RangePreset | ''>('ALL');
  const [serverEntries, setServerEntries] = useState<TimeEntry[]>([]);
  const [timeEntryMeta, setTimeEntryMeta] = useState({ page: 1, limit: TIME_ENTRY_PAGE_LIMIT, total: 0, totalPages: 0 });
  const [timeEntriesLoading, setTimeEntriesLoading] = useState(false);
  const [timeEntriesError, setTimeEntriesError] = useState('');
  const [timeEntriesRequestKey, setTimeEntriesRequestKey] = useState(0);
  const [timeEntrySummary, setTimeEntrySummary] = useState({ entryCount: 0, totalSeconds: 0 });

  const reloadTimeEntryData = useCallback(() => {
    setTimeEntriesRequestKey((current) => current + 1);
  }, []);

  const activeProjects = useMemo(
    () =>
      (data?.projects || []).filter(
        (project) =>
          project.status !== "COMPLETED" && project.status !== "ARCHIVED",
      ),
    [data?.projects],
  );

  useEffect(() => {
    if (!manualOpen || !manualForm.project_id) {
      setManualTasks([]);
      setManualTasksLoaded(false);
      return undefined;
    }

    let active = true;
    setManualTasksLoaded(false);
    listTimerTasks(manualForm.project_id)
      .then((tasks) => {
        if (!active) return;
        setManualTasks(tasks);
        setManualTasksLoaded(true);
      })
      .catch(() => {
        if (!active) return;
        setManualTasks([]);
        setManualTasksLoaded(true);
      });

    return () => {
      active = false;
    };
  }, [manualForm.project_id, manualOpen]);

  useEffect(() => {
    if (filters.project === "ALL") {
      setFilterTasks([]);
      return undefined;
    }

    let active = true;
    listTimerTasks(filters.project)
      .then((tasks) => {
        if (active) setFilterTasks(tasks);
      })
      .catch(() => {
        if (active) setFilterTasks([]);
      });

    return () => {
      active = false;
    };
  }, [filters.project]);

  useEffect(() => {
    setTimeEntryPage(1);
  }, [filters]);

  useEffect(() => {
    if (loading) return undefined;
    let active = true;
    setTimeEntriesLoading(true);
    setTimeEntriesError('');

    const query = {
      clientId: filters.client === 'ALL' ? undefined : filters.client,
      projectId: filters.project === 'ALL' ? undefined : filters.project,
      taskId: filters.task === 'ALL' ? undefined : filters.task,
      from: toApiDateBoundary(filters.from),
      to: toApiDateBoundary(filters.to, true),
      page: timeEntryPage,
      limit: TIME_ENTRY_PAGE_LIMIT,
    };

    Promise.all([listTimeEntriesPage(query), summarizeTimeEntries(query)])
      .then(([pageResult, summary]) => {
        if (!active) return;
        setServerEntries(pageResult.entries);
        setTimeEntryMeta(pageResult.meta);
        setTimeEntrySummary(summary);
      })
      .catch((reason: unknown) => {
        if (!active) return;
        setTimeEntriesError(getErrorMessage(reason, 'ไม่สามารถโหลดรายการเวลาได้'));
      })
      .finally(() => {
        if (active) setTimeEntriesLoading(false);
      });

    return () => {
      active = false;
    };
  }, [data.time_entries, filters.client, filters.from, filters.project, filters.task, filters.to, loading, timeEntriesRequestKey, timeEntryPage]);

  if (loading) return <LoadingState label="LoadingTime entries..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const entries = serverEntries.filter((entry) => entry.ended_at);
  const safeTimeEntryPage = Math.min(timeEntryPage, Math.max(1, timeEntryMeta.totalPages));
  const visibleTimeEntries = entries;

  const openManual = (entry: TimeEntry | null = null) => {
    if (entry) {
      const start = new Date(entry.started_at);
      const end = new Date(entry.ended_at ?? entry.started_at);
      setManualForm({
        ...entry,
        task_id: entry.task_id || "",
        duration_minutes: entry.duration_minutes ?? 0,
        entry_date: localDateValue(start),
        start_time: start.toTimeString().slice(0, 5),
        end_time: end.toTimeString().slice(0, 5),
        manual_mode: "RANGE",
      });
    } else {
      const projectId = activeProjects[0]?.id || "";
      setManualForm(createEmptyManualForm(projectId));
    }
    setFormError("");
    setManualOpen(true);
  };

  const handleManualChange = (
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = event.target;
    const fieldValue =
      event.target instanceof HTMLInputElement &&
      event.target.type === "checkbox"
        ? event.target.checked
        : value;
    setManualForm((current) => ({
      ...current,
      [name]: fieldValue,
      ...(name === "project_id" ? { task_id: "" } : {}),
    }));
  };

  const saveManualEntry = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const project = data.projects.find(
      (item) => item.id === manualForm.project_id,
    );
    if (!project) {
      setFormError("กรุณาเลือกโปรเจกต์");
      return;
    }

    const startedAt = new Date(
      `${manualForm.entry_date}T${manualForm.start_time}:00`,
    );
    let endedAt: Date;
    let duration: number;
    if (manualForm.manual_mode === "RANGE") {
      endedAt = new Date(`${manualForm.entry_date}T${manualForm.end_time}:00`);
      duration = Math.round((endedAt.getTime() - startedAt.getTime()) / 60000);
    } else {
      duration = Number(manualForm.duration_minutes);
      endedAt = new Date(startedAt.getTime() + duration * 60000);
    }
    if (!Number.isFinite(duration) || duration <= 0) {
      setFormError("เวลาเริ่มต้องน้อยกว่าเวลาสิ้นสุดและระยะเวลาต้องมากกว่า 0");
      return;
    }

    try {
      if (manualForm.id) {
        await updateTimeEntry(manualForm.id, {
          projectId: manualForm.project_id,
          ...(manualForm.task_id
            ? { taskId: manualForm.task_id }
            : { clearTask: true }),
          description: manualForm.description,
          startedAt: startedAt.toISOString(),
          endedAt: endedAt.toISOString(),
        });
      } else {
        await createManualTimeEntry({
          projectId: manualForm.project_id,
          taskId: manualForm.task_id || null,
          description: manualForm.description,
          startedAt: startedAt.toISOString(),
          ...(manualForm.manual_mode === "RANGE"
            ? { endedAt: endedAt.toISOString() }
            : { durationSeconds: duration * 60 }),
        });
      }
      await refresh();
      reloadTimeEntryData();
      setManualOpen(false);
    } catch (saveError: unknown) {
      setFormError(getErrorMessage(saveError, "บันทึกรายการเวลาไม่สำเร็จ"));
    }
  };

  const duplicateEntry = async (entry: TimeEntry) => {
    const duplicate = {
      ...entry,
      id: undefined,
      started_at: new Date().toISOString(),
      ended_at: new Date(
        Date.now() + (entry.duration_minutes ?? 0) * 60000,
      ).toISOString(),
    };
    await saveTimeEntry(duplicate);
    reloadTimeEntryData();
  };

  const applyRange = (preset: RangePreset) => {
    if (preset === "ALL") {
      setFilters((current) => ({ ...current, from: "", to: "" }));
      return;
    }
    const now = new Date();
    const local = (date: Date) =>
      new Date(date.getTime() - date.getTimezoneOffset() * 60000)
        .toISOString()
        .slice(0, 10);
    if (preset === "DAY") {
      const today = local(now);
      setFilters((current) => ({ ...current, from: today, to: today }));
      return;
    }
    const weekday = now.getDay() || 7;
    const start = new Date(now);
    start.setDate(start.getDate() - weekday + 1);
    setFilters((current) => ({
      ...current,
      from: local(start),
      to: local(now),
    }));
  };

  return (
    <div className="mx-auto w-full max-w-auto">
      <PageHeader
        title="บันทึกเวลา"
        actions={
          <Button variant="default"
            className="h-10"
            type="button"
            onClick={() => openManual()}
          >
            <FiPlus aria-hidden="true" /> เพิ่มเวลาด้วยตนเอง
          </Button>
        }
      />

      <div className="mb-4 grid grid-cols-[minmax(0,1fr)] items-stretch gap-4">
        <TimerPanel workspace={workspace} onProjectOptionsOpen={() => { void refresh() }} onTimerChanged={reloadTimeEntryData} />
      </div>

      <TimeEntriesCard
        filters={filters}
        rangePreset={rangePreset}
        clients={data.clients}
        projects={data.projects}
        filterTasks={filterTasks}
        entries={visibleTimeEntries}
        summary={timeEntrySummary}
        loading={timeEntriesLoading}
        error={timeEntriesError}
        pagination={{
          page: safeTimeEntryPage,
          totalPages: Math.max(1, timeEntryMeta.totalPages),
          total: timeEntryMeta.total,
          onPageChange: setTimeEntryPage,
        }}
        onRangeChange={(preset) => {
          setRangePreset(preset)
          applyRange(preset)
        }}
        onClientChange={(value) =>
          setFilters((current) => ({
            ...current,
            client: value,
            project: 'ALL',
            task: 'ALL',
          }))
        }
        onProjectChange={(value) =>
          setFilters((current) => ({ ...current, project: value, task: 'ALL' }))
        }
        onTaskChange={(value) =>
          setFilters((current) => ({ ...current, task: value }))
        }
        onDateChange={(field, value) => {
          setRangePreset('')
          setFilters((current) => ({ ...current, [field]: value }))
        }}
        onRetry={reloadTimeEntryData}
        onEdit={openManual}
        onDelete={async (entry) => {
          await deleteTimeEntry(entry.id);
          reloadTimeEntryData();
        }}
        onDuplicate={duplicateEntry}
      />

      <Dialog open={manualOpen} onOpenChange={(open) => { if (!open) setManualOpen(false) }}>
        <DialogContent className="max-h-[calc(100dvh-2rem)] !max-w-4xl overflow-y-auto">
          <DialogHeader><DialogTitle>{manualForm.id ? "แก้ไขTime entries" : "เพิ่มTime entries"}</DialogTitle></DialogHeader>
          <TimeEntryForm
          value={manualForm}
          projects={data.projects}
          tasks={manualTasksLoaded ? manualTasks : []}
          error={formError}
          onChange={handleManualChange}
          onSubmit={saveManualEntry}
          onCancel={() => setManualOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default TimeTrackerPage;
