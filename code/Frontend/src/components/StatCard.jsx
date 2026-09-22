function StatCard({ label, value, change }) {
  return (
    <div className="stat-card">
      <p className="stat-label">{label}</p>
      <p className="stat-value">{value}</p>
      {change && <p className="stat-change positive">↗ {change}</p>}
    </div>
  )
}

export default StatCard