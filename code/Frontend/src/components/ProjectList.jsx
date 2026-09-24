function ProjectList({ projects }) {
  return (
    <div className="panel">
      <h2>Active projects</h2>
      <p className="panel-subtitle">Keep an eye on work in progress</p>
      {projects.map((p, i) => (
        <div className="list-row" key={i}>
          <div>
            <p className="list-title">{p.name}</p>
            <p className="list-sub">{p.client}</p>
          </div>
          <div className="list-progress">
            <span>{p.percent}% complete</span>
            <div className="progress-bar">
              <div className="progress-fill" style={{ width: `${p.percent}%` }}></div>
            </div>
          </div>
          <span className="badge">{p.status}</span>
        </div>
      ))}
    </div>
  )
}

export default ProjectList