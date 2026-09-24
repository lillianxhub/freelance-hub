import Sidebar from '../components/Sidebar'
import '../App.css'

function PlaceholderPage({ title, description }) {
  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="main-content">
        <header className="page-header">
          <div>
            <p className="eyebrow">Freelance Hub</p>
            <h1>{title}</h1>
            <p className="subtitle">{description}</p>
          </div>
        </header>

        <section className="panel empty-state">
          <div className="empty-state-icon" aria-hidden="true">⌁</div>
          <h2>หน้านี้กำลังพัฒนา</h2>
          <p>โครงหน้าและเส้นทางพร้อมแล้ว รอเชื่อมข้อมูลจาก Backend ในขั้นตอนถัดไป</p>
        </section>
      </main>
    </div>
  )
}

export default PlaceholderPage
