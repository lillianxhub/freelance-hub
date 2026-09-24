function ProjectList({ projects }) {
  return (
    <div className="panel">
      <h2>Active projects</h2>
      <p className="panel-subtitle">Keep an eye on work in progress</p>
      {projects.length === 0 && <p className="inline-empty">ยังไม่มีโปรเจกต์ที่กำลังดำเนินการ</p>}
      {projects.map((p) => (
        <div className="list-row" key={p.id}>
          <div>
            <p className="list-title">{p.name}</p>
            <p className="list-sub">{p.client}</p>
          </div>
          <div className="list-progress">
            <span>{p.percent}% complete</span>
            <div className="progress-bar">
              <div
                className={`progress-fill${p.percent >= 80 ? ' warning' : ''}`}
                style={{ width: `${Math.min(Math.max(p.percent, 0), 100)}%` }}
              />
            </div>
          </div>
          <span className="badge">{p.status}</span>
        </div>
      ))}
    </div>
  )
}

export default ProjectList
