import React from 'react';

export default function MetricCard({ title, value, subtitle, color = '#6366f1', icon }) {
  return (
    <div style={{
      background: '#1e1e2e',
      border: `1px solid ${color}33`,
      borderRadius: '12px',
      padding: '20px',
      display: 'flex',
      flexDirection: 'column',
      gap: '6px',
    }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start' }}>
        <span style={{ color: '#64748b', fontSize: '0.85rem', fontWeight: 500 }}>{title}</span>
        {icon && <span style={{ fontSize: '1.3rem' }}>{icon}</span>}
      </div>
      <div style={{ fontSize: '2rem', fontWeight: 700, color }}>{value ?? '—'}</div>
      {subtitle && <div style={{ color: '#475569', fontSize: '0.8rem' }}>{subtitle}</div>}
    </div>
  );
}
