import React from 'react';

const STATUS_COLOR = { true: '#22c55e', false: '#ef4444' };
const STATUS_ICON  = { true: '✓', false: '✗' };

function timeAgo(dateStr) {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const min  = Math.floor(diff / 60000);
  if (min < 1)  return 'hace un momento';
  if (min < 60) return `hace ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24)   return `hace ${h}h`;
  return `hace ${Math.floor(h / 24)}d`;
}

export default function ActivityFeed({ items = [] }) {
  if (!items.length) return (
    <div style={{ color:'#475569', textAlign:'center', padding:'20px 0', fontSize:'0.9rem' }}>
      Sin actividad reciente
    </div>
  );

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:'8px' }}>
      {items.map((item, i) => (
        <div key={i} style={{
          display: 'flex', alignItems: 'flex-start', gap: '12px',
          padding: '10px', borderRadius: '8px', background: '#16161f',
          border: '1px solid #2a2a3e',
        }}>
          <span style={{
            width: '22px', height: '22px', borderRadius: '50%', flexShrink: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: '0.75rem', fontWeight: 700,
            background: `${STATUS_COLOR[item.valid]}22`,
            color: STATUS_COLOR[item.valid],
          }}>
            {STATUS_ICON[item.valid]}
          </span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{
              color: '#cbd5e1', fontSize: '0.85rem',
              whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            }}>
              {item.queryPreview}
            </div>
            <div style={{ color: '#475569', fontSize: '0.75rem', marginTop: '2px' }}>
              {item.statementType} · {item.databaseType} · {timeAgo(item.analyzedAt)}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
