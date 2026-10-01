import { Card } from '../../../components/ui/card'
import { Button } from '../../../components/ui/button'
import { Input } from '../../../components/ui/input'
import { NativeSelect } from '../../../components/ui/native-select'
import { useCallback, useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react";
import { FiBriefcase, FiPlus } from "react-icons/fi";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../../components/ui/dialog";
import PageHeader from "../../../components/PageHeader";
import {
  EmptyState,
  ErrorState,
  LoadingState,
} from "../../../components/ViewState";
import { useProjects } from "../../useProjects";
import type { Project } from "../../../types/project";
import type { ProjectStatus } from "../../../types/project";
import type {
  ProjectDraft,
  ProjectFilter,
  ProjectSort,
} from "../../../types/projectsPage";
import ProjectForm from "../../components/ProjectForm";
import ProjectCard from "../../components/ProjectCard";
import FilterBar from '../../../components/FilterBar'
import { getErrorMessage } from "../../../api/apiError";
import { changeProjectStatus } from "../../../services/project";
import { listProjectsPage, type ProjectListFilters } from "../../../services/workspace";
import type { NotificationMessage } from "../../../types/notification";
import { toast } from 'sonner';
import type { ApiMeta } from "../../../types/api";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "../../../components/ui/pagination";

const emptyForm: ProjectDraft = {
  name: "",
  client_id: "",
  description: "",
  color: "#4F6BFF",
  status: "PLANNED",
  billing_type: "HOURLY",
  hourly_rate: 850,
  fixed_price: "",
  budget_hours: 40,
  budget_amount: "",
  currency: "THB",
  start_date: "",
  end_date: "",
};

const projectPageLimit = 10;

const projectSortParams: Record<ProjectSort, Pick<ProjectListFilters, 'sortBy' | 'direction'>> = {
  UPDATED_DESC: { sortBy: 'update_at', direction: 'DESC' },
  NAME_ASC: { sortBy: 'project_name', direction: 'ASC' },
  END_ASC: { sortBy: 'end_date', direction: 'ASC' },
};

function getPaginationItems(currentPage: number, totalPages: number): Array<number | "ellipsis"> {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  const pages = new Set([1, totalPages, currentPage - 1, currentPage, currentPage + 1]);
  return [...pages]
    .filter((item) => item >= 1 && item <= totalPages)
    .sort((a, b) => a - b)
    .flatMap((item, index, items) =>
      index > 0 && item - items[index - 1] > 1 ? ["ellipsis" as const, item] : [item],
    );
}

function ProjectsPage() {
  const { data, loading, error, refresh, save } = useProjects();
  const [query, setQuery] = useState("");
  const [clientId, setClientId] = useState("ALL");
  const [status, setStatus] = useState<ProjectFilter>("ALL");
  const [sortBy, setSortBy] = useState<ProjectSort>("UPDATED_DESC");
  const [page, setPage] = useState(1);
  const [pageProjects, setPageProjects] = useState<Project[]>([]);
  const [pageMeta, setPageMeta] = useState<ApiMeta>({
    page: 1,
    limit: projectPageLimit,
    total: 0,
    totalPages: 1,
  });
  const [pageLoading, setPageLoading] = useState(true);
  const [pageError, setPageError] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const [changingProjectId, setChangingProjectId] = useState<string | null>(null);
  const showToast = ({ success, message }: NotificationMessage) => {
    toast[success ? 'success' : 'error'](message);
  };
  const latestRequest = useRef(0);

  const loadProjectPage = useCallback(async (requestedPage: number) => {
    const requestId = ++latestRequest.current;
    setPageLoading(true);
    setPageError("");
    try {
      const result = await listProjectsPage(requestedPage, projectPageLimit, {
        clientId: clientId === 'ALL' ? undefined : clientId,
        search: query,
        status: status === 'ALL' ? undefined : status,
        ...projectSortParams[sortBy],
      });
      if (requestId !== latestRequest.current) return;
      if (requestedPage > Math.max(1, result.meta.totalPages)) {
        setPage(Math.max(1, result.meta.totalPages));
        return;
      }
      setPageProjects(result.projects);
      setPageMeta(result.meta);
    } catch (loadError) {
      if (requestId === latestRequest.current) {
        setPageError(getErrorMessage(loadError, "ไม่สามารถโหลดโปรเจกต์ได้"));
      }
    } finally {
      if (requestId === latestRequest.current) setPageLoading(false);
    }
  }, [clientId, query, sortBy, status]);

  useEffect(() => {
    void loadProjectPage(page);
  }, [loadProjectPage, page]);

  const projects = pageProjects;
  const totalPages = Math.max(1, pageMeta.totalPages);
  const safePage = Math.min(pageMeta.page, totalPages);
  const paginationItems = getPaginationItems(safePage, totalPages);

  const openCreate = () => {
    setForm({
      ...emptyForm,
      client_id:
        data.clients.find((client) => client.status === "ACTIVE")?.id || "",
    });
    setFormError("");
    setModalOpen(true);
  };

  const openEdit = (project: Project) => {
    setForm({
      ...emptyForm,
      ...project,
      hourly_rate: project.hourly_rate ?? "",
      fixed_price: project.fixed_price ?? "",
      budget_hours: project.budget_hours ?? "",
      budget_amount: project.budget_amount ?? "",
    });
    setFormError("");
    setModalOpen(true);
  };

  const handleChange = (
    event: ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.name.trim() || !form.client_id) {
      setFormError("กรุณาระบุชื่อโปรเจกต์และลูกค้า");
      return;
    }
    // if (form.billing_type === "HOURLY" && Number(form.hourly_rate) <= 0) {
    //   setFormError("โปรเจกต์รายชั่วโมงต้องมีอัตราต่อชั่วโมงมากกว่า 0");
    //   return;
    // }
    // if (form.billing_type === "FIXED_PRICE" && Number(form.fixed_price) <= 0) {
    //   setFormError("โปรเจกต์เหมาจ่ายต้องมีมูลค่างานมากกว่า 0");
    //   return;
    // }
    if (form.start_date && form.end_date && form.end_date < form.start_date) {
      setFormError("วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่ม");
      return;
    }

    setSaving(true);
    setFormError("");
    try {
      await save("projects", {
        ...form,
        name: form.name.trim(),
        hourly_rate:
          form.billing_type === "HOURLY" ? Number(form.hourly_rate) : null,
        fixed_price:
          form.billing_type === "FIXED_PRICE" ? Number(form.fixed_price) : null,
        budget_hours:
          form.budget_hours === "" ? null : Number(form.budget_hours),
        budget_amount:
          form.budget_amount === "" ? null : Number(form.budget_amount),
      });
      setModalOpen(false);
      if (page === 1) await loadProjectPage(1);
      else setPage(1);
    } catch (err) {
      setFormError(getErrorMessage(err, "บันทึกโปรเจกต์ไม่สำเร็จ"));
    } finally {
      setSaving(false);
    }
  };

  const updateProjectStatus = async (project: Project, nextStatus: ProjectStatus) => {
    if (nextStatus === project.status) return;
    setChangingProjectId(project.id);
    try {
      await changeProjectStatus(project.id, nextStatus);
      await loadProjectPage(safePage);
      showToast({ success: true, message: "อัปเดตสถานะโปรเจกต์เรียบร้อยแล้ว" });
    } catch (statusError) {
      showToast({
        success: false,
        message: getErrorMessage(statusError, "ไม่สามารถอัปเดตสถานะโปรเจกต์ได้"),
      });
    } finally {
      setChangingProjectId(null);
    }
  };

  if (loading || pageLoading) return <LoadingState label="กำลังโหลดโปรเจกต์..." />;
  if (error || pageError) {
    return (
      <ErrorState
        message={error || pageError}
        onRetry={async () => {
          await refresh();
          await loadProjectPage(page);
        }}
      />
    );
  }

  return (
    <div className="mx-auto w-full max-w-screen-2xl">
      <PageHeader
        title="โปรเจกต์ของคุณ"
        actions={
          <Button variant="default"
            className="h-10"
            type="button"
            onClick={openCreate}
          >
            <FiPlus aria-hidden="true" /> เพิ่มโปรเจกต์
          </Button>
        }
      />

      <FilterBar
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          setPage(1);
        }}
        placeholder="ค้นหาโปรเจกต์หรือลูกค้า"
        searchAriaLabel="ค้นหาโปรเจกต์หรือลูกค้า"
      >
        <NativeSelect
          value={clientId}
          onChange={(event) => {
            setClientId(event.target.value);
            setPage(1);
          }}
          aria-label="กรองตามลูกค้า"
        >
          <option value="ALL">ทุกลูกค้า</option>
          {data.clients.map((client) => (
            <option key={client.id} value={client.id}>
              {client.company_name || client.name}
            </option>
          ))}
        </NativeSelect>
        <NativeSelect
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as ProjectFilter);
            setPage(1);
          }}
        >
          <option value="ALL">ทุกสถานะ</option>
          <option value="PLANNED">วางแผน</option>
          <option value="ACTIVE">กำลังทำ</option>
          <option value="ON_HOLD">พักงาน</option>
          <option value="COMPLETED">เสร็จสิ้น</option>
          <option value="ARCHIVED">เก็บถาวร</option>
        </NativeSelect>
        <NativeSelect
          value={sortBy}
          onChange={(event) => {
            setSortBy(event.target.value as ProjectSort);
            setPage(1);
          }}
        >
          <option value="UPDATED_DESC">อัปเดตล่าสุด</option>
          <option value="NAME_ASC">ชื่อ A–Z</option>
          <option value="END_ASC">กำหนดส่งใกล้สุด</option>
        </NativeSelect>
      </FilterBar>

      {projects.length === 0 ? (
        <Card asChild>
          <section className="rounded-xl border border-border bg-surface p-5 shadow-soft ring-0">
          <EmptyState
            icon={<FiBriefcase aria-hidden="true" />}
            title="ยังไม่พบโปรเจกต์"
            description="สร้างโปรเจกต์แรกหรือปรับตัวกรอง"
            action={
              <Button variant="default"
                className="h-10"
                type="button"
                onClick={openCreate}
              >
                เพิ่มโปรเจกต์
              </Button>
            }
          />
        </section></Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          {projects.map((project) => {
            const client = data.clients.find(
              (item) => item.id === project.client_id,
            );
            const clientName = project.client_name || client?.company_name || client?.name || "ไม่พบลูกค้า";
            return <ProjectCard
              key={project.id}
              project={project}
              clientName={clientName}
              changing={changingProjectId === project.id}
              onEdit={openEdit}
              onStatusChange={(nextProject, status) => { void updateProjectStatus(nextProject, status) }}
            />;
          })}
        </div>
      )}

      <Pagination className="mt-6">
          <PaginationContent>
            <PaginationItem>
              <PaginationPrevious
                href={`?page=${safePage - 1}`}
                text="ก่อนหน้า"
                aria-disabled={safePage === 1}
                className={safePage === 1 ? "pointer-events-none opacity-50" : undefined}
                onClick={(event) => {
                  event.preventDefault();
                  if (safePage > 1) setPage(safePage - 1);
                }}
              />
            </PaginationItem>
            {paginationItems.map((item, index) => (
              <PaginationItem key={`${item}-${index}`}>
                {item === "ellipsis" ? (
                  <PaginationEllipsis />
                ) : (
                  <PaginationLink
                    href={`?page=${item}`}
                    isActive={item === safePage}
                    onClick={(event) => {
                      event.preventDefault();
                      setPage(item);
                    }}
                  >
                    {item}
                  </PaginationLink>
                )}
              </PaginationItem>
            ))}
            <PaginationItem>
              <PaginationNext
                href={`?page=${safePage + 1}`}
                text="ถัดไป"
                aria-disabled={safePage === totalPages}
                className={safePage === totalPages ? "pointer-events-none opacity-50" : undefined}
                onClick={(event) => {
                  event.preventDefault();
                  if (safePage < totalPages) setPage(safePage + 1);
                }}
              />
            </PaginationItem>
          </PaginationContent>
      </Pagination>

      <Dialog open={modalOpen} onOpenChange={(open) => { if (!open) setModalOpen(false) }}>
        <DialogContent className="!max-w-3xl max-h-[calc(100dvh-2rem)] overflow-y-auto">
          <DialogHeader><DialogTitle>{form.id ? "แก้ไขโปรเจกต์" : "เพิ่มโปรเจกต์"}</DialogTitle></DialogHeader>
          <ProjectForm
          value={form}
          clients={data.clients}
          error={formError}
          saving={saving}
          onChange={handleChange}
          onSubmit={handleSubmit}
          onCancel={() => setModalOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default ProjectsPage;
