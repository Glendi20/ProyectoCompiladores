import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import MetricCard   from '../components/dashboard/MetricCard';
import StatsChart   from '../components/dashboard/StatsChart';
import ActivityFeed from '../components/dashboard/ActivityFeed';
import './Dashboard.css';

export default function Dashboard() {
  const { user } = useAuth();
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState('');

  useEffect(() => {
    api.get('/dashboard/my-stats')
      .then(r => setData(r.data))
      .catch(() => setError('No se pudo cargar el dashboard'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="dash-loading">Cargando dashboard...</div>;
  if (error)   return <div className="dash-error">{error}</div>;

  // El API devuelve: totalQueries, successful, failed, successRate,
  //                  byStatementType {}, byDatabase {}, recentQueries []
  const stmtStats = Object.entries(data?.byStatementType ?? {})
    .map(([statementType, count]) => ({ statementType, count }));

  return (
    <div className="dashboard">
      <div className="dash-header">
        <div>
          <h2 className="dash-title">Hola, {user?.nombre} 👋</h2>
          <p className="dash-subtitle">Tu progreso en DataQuery SQL Compiler</p>
        </div>
      </div>

      <div className="dash-metrics">
        <MetricCard title="Total de consultas"    value={data?.totalQueries}  icon="📊" color="#6366f1" subtitle="Desde que te registraste" />
        <MetricCard title="Consultas correctas"   value={data?.successful}    icon="✅" color="#22c55e" subtitle={`${data?.successRate ?? 0}% de éxito`} />
        <MetricCard title="Errores encontrados"   value={data?.failed}        icon="⚠️" color="#ef4444" subtitle="Para aprender de ellos" />
        <MetricCard title="SELECT * usados"       value={data?.selectStarCount} icon="💡" color="#f59e0b" subtitle="Mala práctica a evitar" />
      </div>

      <div className="dash-grid">
        <div className="dash-card">
          <h3 className="dash-card-title">Tipos de consulta</h3>
          <StatsChart data={stmtStats} />
        </div>

        <div className="dash-card">
          <h3 className="dash-card-title">Base de datos favorita</h3>
          <StatsChart data={Object.entries(data?.byDatabase ?? {}).map(([statementType, count]) => ({ statementType, count }))} />
        </div>
      </div>

      <div className="dash-card" style={{ marginTop: '20px' }}>
        <h3 className="dash-card-title">Actividad reciente</h3>
        <ActivityFeed items={data?.recentQueries ?? []} />
      </div>
    </div>
  );
}
