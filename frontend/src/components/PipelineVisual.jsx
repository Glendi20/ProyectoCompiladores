import React from 'react';
import './PipelineVisual.css';

const PHASES = [
  { key: 'lexer',     label: 'Análisis Léxico',     icon: '🔤', num: 1 },
  { key: 'parser',    label: 'Análisis Sintáctico',  icon: '🌳', num: 2 },
  { key: 'semantic',  label: 'Análisis Semántico',   icon: '🔍', num: 3 },
  { key: 'optimizer', label: 'Optimización IA',      icon: '⚡', num: 4 },
];

function getPhaseInfo(key, result, loading) {
  if (loading) return { status: 'loading', metric: 'procesando...' };
  if (!result)  return { status: 'idle',   metric: '—' };

  const tkCount = result.tokens?.filter(t => t.type !== 'END_OF_FILE').length || 0;

  switch (key) {
    case 'lexer':
      if (tkCount === 0) return { status: 'error', metric: 'sin tokens' };
      return { status: 'ok', metric: `${tkCount} tokens` };
    case 'parser':
      if (!result.ast) return { status: 'error', metric: 'error sintáctico' };
      return { status: 'ok', metric: result.statementType || 'AST ok' };
    case 'semantic':
      if (result.errors?.length)   return { status: 'error', metric: `${result.errors.length} error(s)` };
      if (result.warnings?.length) return { status: 'warn',  metric: `${result.warnings.length} advertencia(s)` };
      return { status: 'ok', metric: 'sin errores' };
    case 'optimizer':
      if (!result.ast) return { status: 'idle', metric: '—' };
      return { status: 'ok', metric: `${result.suggestions?.length || 0} sugerencia(s)` };
    default:
      return { status: 'idle', metric: '—' };
  }
}

export default function PipelineVisual({ result, loading }) {
  return (
    <div className="pv-bar">
      <span className="pv-label">PIPELINE</span>
      <div className="pv-stages">
        {PHASES.map((phase, i) => {
          const { status, metric } = getPhaseInfo(phase.key, result, loading);
          const prevStatus = i > 0 ? getPhaseInfo(PHASES[i - 1].key, result, loading).status : 'ok';
          const connectorDone = i > 0 && prevStatus === 'ok';
          return (
            <React.Fragment key={phase.key}>
              {i > 0 && (
                <div className={`pv-connector ${connectorDone ? 'done' : ''}`}>
                  <span className="pv-arrow">›</span>
                </div>
              )}
              <div className={`pv-stage s-${status}`}>
                <div className="pv-stage-top">
                  <span className={`pv-num n-${status}`}>{phase.num}</span>
                  <span className="pv-icon">{phase.icon}</span>
                  <span className={`pv-dot d-${status}`} />
                </div>
                <div className="pv-name">{phase.label}</div>
                <div className={`pv-metric m-${status}`}>{metric}</div>
              </div>
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
