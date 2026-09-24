function TaskList({ tasks }) {
  return (
    <div className="panel">
      <h2>Upcoming tasks</h2>
      <p className="panel-subtitle">Keep your next actions visible</p>
      {tasks.length === 0 && <p className="inline-empty">ยังไม่มีงานที่กำลังจะถึง</p>}
      {tasks.map((t) => (
        <div className="list-row" key={t.id}>
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
