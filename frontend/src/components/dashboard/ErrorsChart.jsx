import React from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const COLORS = ['#ef4444','#f97316','#eab308','#6366f1','#8b5cf6'];

export default function ErrorsChart({ data = [] }) {
  if (!data.length) return (
    <div style={{ color:'#475569', textAlign:'center', padding:'40px 0', fontSize:'0.9rem' }}>
      Sin errores registrados aún
    </div>
  );

  const chartData = data.slice(0, 5).map(e => ({
    name: e.errorPattern?.length > 20 ? e.errorPattern.substring(0, 20) + '…' : e.errorPattern,
    count: e.totalCount,
  }));

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={chartData} margin={{ top: 5, right: 10, left: -20, bottom: 5 }}>
        <XAxis dataKey="name" tick={{ fill:'#64748b', fontSize:11 }} />
        <YAxis tick={{ fill:'#64748b', fontSize:11 }} />
        <Tooltip
          contentStyle={{ background:'#1e1e2e', border:'1px solid #2a2a3e', borderRadius:'8px' }}
          labelStyle={{ color:'#e2e8f0' }}
          itemStyle={{ color:'#94a3b8' }}
        />
        <Bar dataKey="count" radius={[4,4,0,0]}>
          {chartData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
