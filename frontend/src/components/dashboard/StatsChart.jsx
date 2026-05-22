import React from 'react';
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = ['#6366f1','#22c55e','#f97316','#eab308','#8b5cf6','#06b6d4'];

export default function StatsChart({ data = [] }) {
  if (!data.length) return (
    <div style={{ color:'#475569', textAlign:'center', padding:'40px 0', fontSize:'0.9rem' }}>
      Sin consultas registradas aún
    </div>
  );

  const chartData = data.map(s => ({ name: s.statementType, value: s.count }));

  return (
    <ResponsiveContainer width="100%" height={200}>
      <PieChart>
        <Pie data={chartData} cx="50%" cy="50%" innerRadius={50} outerRadius={80}
             dataKey="value" paddingAngle={3}>
          {chartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
        </Pie>
        <Tooltip
          contentStyle={{ background:'#1e1e2e', border:'1px solid #2a2a3e', borderRadius:'8px' }}
          itemStyle={{ color:'#94a3b8' }}
        />
        <Legend wrapperStyle={{ color:'#64748b', fontSize:'0.8rem' }} />
      </PieChart>
    </ResponsiveContainer>
  );
}
