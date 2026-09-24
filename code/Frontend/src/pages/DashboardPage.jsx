import { useState, useEffect } from 'react'
import Sidebar from '../components/Sidebar'
import StatCard from '../components/StatCard'
import ActivityChart from '../components/ActivityChart'
import FocusTimer from '../components/FocusTimer'
import ProjectList from '../components/ProjectList'
import TaskList from '../components/TaskList'
import { getDashboardData } from '../data/mockDashboardData'
import '../App.css'

function DashboardPage() {
  const [data, setData] = useState(null)

  useEffect(() => {
    getDashboardData().then((result) => setData(result))
  }, [])

  if (!data) {
    return <div className="dashboard-layout"><Sidebar /><main className="main-content">กำลังโหลด...</main></div>
  }

  return (
    <div className="dashboard-layout">
      <Sidebar />
      <main className="main-content">
        <h1>Good morning, {data.greeting}</h1>
        <p className="subtitle">Here's what's happening across your freelance business.</p>

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