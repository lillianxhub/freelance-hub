import { BarChart, Bar, XAxis, YAxis, CartesianGrid, ResponsiveContainer } from 'recharts'

function ActivityChart({ data }) {
  return (
    <div className="panel">
      <h2>Activity overview</h2>
      <p className="panel-subtitle">Hours tracked across your projects</p>
      <ResponsiveContainer width="100%" height={250}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#3a3a44" />
          <XAxis dataKey="day" stroke="#9999a5" />
          <YAxis stroke="#9999a5" />
          <Bar dataKey="focus" stackId="a" fill="#4f46e5" radius={[4, 4, 0, 0]} />
          <Bar dataKey="other" stackId="a" fill="#3a3a5c" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export default ActivityChart