import PageHeader from '../components/PageHeader'
import { EmptyState } from '../components/ViewState'

function PlaceholderPage({ title, description }) {
  return (
    <div className="page-view">
      <PageHeader eyebrow="Workspace" title={title} description={description} />
      <section className="panel">
        <EmptyState icon="⌁" title="กำลังเตรียมหน้านี้" description="โครงสร้างข้อมูลพร้อมสำหรับ Demo mode และ Supabase แล้ว" />
      </section>
    </div>
  )
}

export default PlaceholderPage
