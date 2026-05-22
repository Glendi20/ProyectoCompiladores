import React, { useEffect, useState } from 'react';
import api from '../services/api';
import MetricCard   from '../components/dashboard/MetricCard';
import ErrorsChart  from '../components/dashboard/ErrorsChart';
import StatsChart   from '../components/dashboard/StatsChart';
import ActivityFeed from '../components/dashboard/ActivityFeed';
import './Dashboard.css';

export default function ProfessorDashboard() {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    api.get('/dashboard/global/stats')
      .then(r => setData(r.data))
      .catch(() => setError('No se pudo cargar la vista global'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="dash-loading">Cargando estadísticas globales...</div>;
  if (error)   return <div className="dash-error">{error}</div>;

  const stmtStats = Object.entries(data?.byStatementType ?? {})
    .map(([statementType, count]) => ({ statementType, count }));

  const topErrors = (data?.topErrors ?? []).map(e => ({
    errorPattern: e.errorPattern,
    totalCount:   e.occurrenceCount,
  }));

  return (
    <div className="dashboard">
      <div className="dash-header">
        <div>
          <h2 className="dash-title">Vista Administrador — Estadísticas Globales</h2>
          <p className="dash-subtitle">Actividad de todos los usuarios en el sistema</p>
        </div>
      </div>

      <div className="dash-metrics">
        <MetricCard title="Total de usuarios"   value={data?.totalUsers}    icon="👥" color="#6366f1" subtitle="Registrados en el sistema" />
        <MetricCard title="Consultas totales"   value={data?.totalQueries}  icon="📊" color="#06b6d4" subtitle="De todos los estudiantes" />
        <MetricCard title="Tasa de éxito"       value={`${data?.successRate ?? 0}%`} icon="✅" color="#22c55e" subtitle="Consultas correctas" />
        <MetricCard title="Errores totales"     value={data?.failed}        icon="⚠️" color="#ef4444" subtitle="A nivel global" />
      </div>

      <div className="dash-grid">
        <div className="dash-card">
          <h3 className="dash-card-title">Tipos de consulta (global)</h3>
          <StatsChart data={stmtStats} />
        </div>
        <div className="dash-card">
          <h3 className="dash-card-title">Errores más frecuentes</h3>
          <ErrorsChart data={topErrors} />
        </div>
      </div>

      {(data?.usersProgress ?? []).length > 0 && (
        <div className="dash-card" style={{ marginTop: '20px' }}>
          <h3 className="dash-card-title">Progreso por estudiante</h3>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #2a2a3e' }}>
                  {['Estudiante','Email','Consultas','Correctas','Éxito'].map(h => (
                    <th key={h} style={{ textAlign:'left', padding:'8px 12px', color:'#64748b', fontWeight:600 }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {data.usersProgress.map((u, i) => (
                  <tr key={i} style={{ borderBottom: '1px solid #1a1a2e' }}>
                    <td style={{ padding:'8px 12px', color:'#e2e8f0' }}>{u.nombre}</td>
                    <td style={{ padding:'8px 12px', color:'#64748b' }}>{u.email}</td>
                    <td style={{ padding:'8px 12px', color:'#94a3b8' }}>{u.totalQueries}</td>
                    <td style={{ padding:'8px 12px', color:'#22c55e' }}>{u.successful}</td>
                    <td style={{ padding:'8px 12px', color:'#6366f1', fontWeight:600 }}>{u.successRate}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="dash-card" style={{ marginTop: '20px' }}>
        <h3 className="dash-card-title">Actividad reciente (global)</h3>
        <ActivityFeed items={data?.recentQueries ?? []} />
      </div>
    </div>
  );
}
