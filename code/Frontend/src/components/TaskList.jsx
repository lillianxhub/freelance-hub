function TaskList({ tasks }) {
  return (
    <div className="panel">
      <h2>Upcoming tasks</h2>
      <p className="panel-subtitle">Keep your next actions visible</p>
      {tasks.map((t, i) => (
        <div className="list-row" key={i}>
          <div>
            <p className="list-title">{t.title}</p>
            <p className="list-sub">{t.project}</p>
          </div>
          <span className="badge">{t.status}</span>
        </div>
      ))}
    </div>
  )
}

export default TaskList