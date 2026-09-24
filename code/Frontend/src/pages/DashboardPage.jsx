import { useState, useEffect } from 'react'
import Sidebar from '../components/Sidebar'
import StatCard from '../components/StatCard'
import ActivityChart from '../components/ActivityChart'
import FocusTimer from '../components/FocusTimer'
import ProjectList from '../components/ProjectList'
import TaskList from '../components/TaskList'
import { getDashboardData } from '../data/mockDashboardData'
import '../App.css'

function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('currentUser'))
  } catch {
    localStorage.removeItem('currentUser')
    return null
  }
}

function DashboardPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    let isActive = true

    getDashboardData()
      .then((result) => {
        if (isActive) setData(result)
      })
      .catch(() => {
        if (isActive) setError('ไม่สามารถโหลดข้อมูล Dashboard ได้ กรุณาลองใหม่อีกครั้ง')
      })

    return () => {
      isActive = false
    }
  }, [retryKey])

  const handleRetry = () => {
    setData(null)
    setError('')
    setRetryKey((key) => key + 1)
  }

  if (error) {
    return (
      <div className="dashboard-layout">
        <Sidebar />
        <main className="main-content centered-state">
          <section className="panel error-state" role="alert">
            <div className="state-icon" aria-hidden="true">!</div>
            <h2>โหลดข้อมูลไม่สำเร็จ</h2>
            <p>{error}</p>
            <button className="btn-primary state-button" type="button" onClick={handleRetry}>ลองใหม่</button>
          </section>
        </main>
      </div>
    )
  }

  if (!data) {
    return (
      <div className="dashboard-layout">
        <Sidebar />
        <main className="main-content" aria-busy="true" aria-label="กำลังโหลด Dashboard">
          <div className="skeleton skeleton-title" />
          <div className="skeleton skeleton-subtitle" />
          <div className="summary-cards">
            {[1, 2, 3, 4].map((item) => <div className="stat-card skeleton-card" key={item} />)}
          </div>
          <div className="dashboard-grid">
            <div className="panel skeleton-panel" />
            <div className="panel skeleton-panel" />
          </div>
        </main>
      </div>
    )
  }

  const currentUser = getStoredUser()
  const greeting = currentUser?.fullName || data.greeting

  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="main-content">
        <p className="eyebrow">Dashboard overview</p>
        <h1>สวัสดี, {greeting}</h1>
        <p className="subtitle">ภาพรวมการทำงานฟรีแลนซ์ของคุณในสัปดาห์นี้</p>

        <div className="summary-cards">
          <StatCard label="Tracked this week" value={data.trackedThisWeek} change={data.trackedChange} />
          <StatCard label="Time utilization" value={data.utilization} change={data.utilizationChange} />
          <StatCard label="Active projects" value={data.activeProjects} />
          <StatCard label="Tasks completed" value={data.tasksCompleted} />
        </div>

        <div className="dashboard-grid">
          <ActivityChart data={data.activityData} />
          <FocusTimer />
        </div>

        <div className="dashboard-grid">
          <ProjectList projects={data.activeProjectsList} />
          <TaskList tasks={data.upcomingTasks} />
        </div>
      </main>
    </div>
  )
}

export default DashboardPage
