import React, { useState } from 'react';
import './ResultPanel.css';

const TABS = ['Resultado', 'Tokens', 'AST', 'Sugerencias', 'Log', 'Símbolos'];

export default function ResultPanel({ result, loading, schema }) {
  const [activeTab, setActiveTab] = useState(0);

  if (loading) return (
    <div className="result-panel center">
      <div className="spinner" /><p>Analizando consulta...</p>
    </div>
  );

  if (!result) return (
    <div className="result-panel center">
      <span style={{fontSize:'2.5rem'}}>📝</span>
      <p style={{color:'#4a5568',marginTop:'10px',textAlign:'center',fontSize:'0.9rem'}}>
        Escribe una consulta SQL y presiona "Analizar Consulta" para ver los resultados.
      </p>
    </div>
  );

  return (
    <div className="result-panel">
      <div className={`result-badge ${result.valid ? 'valid' : 'invalid'}`}>
        {result.valid ? '✅ Consulta válida' : '❌ Consulta con errores'}
      </div>

      {result.dialectNote && (
        <div className="dialect-note">ℹ️ {result.dialectNote}</div>
      )}

      {result.optimizedQuery && (
        <div className="optimized-query">
          <strong>Versión optimizada sugerida:</strong>
          <code>{result.optimizedQuery}</code>
        </div>
      )}

      <div className="tabs">
        {TABS.map((label, i) => (
          <button key={i} className={`tab ${activeTab === i ? 'active' : ''}`} onClick={() => setActiveTab(i)}>
            {label}
            {i === 3 && result.suggestions?.length > 0 && (
              <span className="tab-badge">{result.suggestions.length}</span>
            )}
          </button>
        ))}
      </div>

      <div className="tab-content">
        {activeTab === 0 && <ResultTab result={result} />}
        {activeTab === 1 && <TokensTab tokens={result.tokens} />}
        {activeTab === 2 && <AstTab ast={result.ast} />}
        {activeTab === 3 && <SuggestionsTab suggestions={result.suggestions} />}
        {activeTab === 4 && <CompilerLogTab result={result} />}
        {activeTab === 5 && <SymbolTableTab schema={schema} />}
      </div>
    </div>
  );
}

function ResultTab({ result }) {
  return (
    <div className="result-tab">
      {result.errors?.length > 0 && (
        <div className="error-list">
          <h4>Errores:</h4>
          {result.errors.map((e, i) => <div key={i} className="error-item">⛔ {e}</div>)}
        </div>
      )}
      {result.warnings?.length > 0 && (
        <div className="warning-list">
          <h4>Advertencias:</h4>
          {result.warnings.map((w, i) => <div key={i} className="warning-item">⚠️ {w}</div>)}
        </div>
      )}
      {result.valid && !result.errors?.length && (
        <div className="success-msg">
          ✅ La consulta pasó el análisis léxico, sintáctico y semántico correctamente.
        </div>
      )}
    </div>
  );
}

function TokensTab({ tokens }) {
  if (!tokens?.length) return <p className="empty-msg">No hay tokens disponibles.</p>;
  const filtered = tokens.filter(t => t.type !== 'END_OF_FILE');
  return (
    <div className="tokens-tab">
      <p className="tokens-count">{filtered.length} token(s) encontrados:</p>
      <div className="tokens-grid">
        {filtered.map((t, i) => (
          <div key={i} className={`token-chip type-${t.type.toLowerCase()}`}>
            <span className="token-type">{t.type}</span>
            {t.value && <span className="token-value">'{t.value}'</span>}
            <span className="token-pos">L{t.line}:C{t.column}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function AstTab({ ast }) {
  if (!ast) return <p className="empty-msg">AST no disponible (errores sintácticos).</p>;
  return (
    <div className="ast-tab">
      <p className="ast-hint">Árbol de Sintaxis Abstracta — estructura interna de tu consulta:</p>
      <pre className="ast-tree">{ast}</pre>
    </div>
  );
}

function SuggestionsTab({ suggestions }) {
  if (!suggestions?.length) return <p className="empty-msg">No hay sugerencias. Analiza una consulta primero.</p>;
  const icons  = { optimization: '⚡', 'best-practice': '📚', dialect: '🔧', danger: '⛔', personal: '🎯' };
  const colors = { optimization: '#c05621', 'best-practice': '#2b6cb0', dialect: '#276749', danger: '#991b1b', personal: '#6d28d9' };

  return (
    <div className="suggestions-tab">
      {suggestions.map((s, i) => (
        <div key={i} className="suggestion-card" style={{borderLeftColor: colors[s.type] || '#4a5568'}}>
          <div className="suggestion-header">
            <span>{icons[s.type] || '💡'}</span>
            <strong>{s.title}</strong>
            <span className="type-tag" style={{background: colors[s.type] || '#4a5568'}}>{s.type}</span>
          </div>
          <p className="suggestion-msg">{s.message}</p>
          {s.example && (
            <div className="suggestion-example">
              <span className="example-label">Ejemplo:</span>
              <code>{s.example}</code>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

// ── Log de compilación ──────────────────────────────────────────────────────

function buildLog(result) {
  const logs = [];
  let t = 1;
  const fmt = n => String(n).padStart(2, '0');

  logs.push({ time: `00:${fmt(t++)}`, phase: 'lexer',     level: 'info',  msg: 'Iniciando análisis léxico...' });
  const tkCount = result.tokens?.filter(tk => tk.type !== 'END_OF_FILE').length || 0;
  logs.push({ time: `00:${fmt(t++)}`, phase: 'lexer',     level: 'ok',    msg: `${tkCount} tokens generados correctamente` });

  logs.push({ time: `00:${fmt(t++)}`, phase: 'parser',    level: 'info',  msg: 'Construyendo árbol sintáctico (AST)...' });
  if (result.ast) {
    logs.push({ time: `00:${fmt(t++)}`, phase: 'parser',  level: 'ok',    msg: `AST generado — tipo: ${result.statementType || 'SELECT'}` });
  } else {
    logs.push({ time: `00:${fmt(t++)}`, phase: 'parser',  level: 'error', msg: 'No se pudo generar el AST' });
  }

  logs.push({ time: `00:${fmt(t++)}`, phase: 'semantic',  level: 'info',  msg: 'Validando tablas, columnas y tipos...' });
  if (result.errors?.length) {
    result.errors.forEach(e => {
      logs.push({ time: `00:${fmt(t++)}`, phase: 'semantic', level: 'error', msg: e });
    });
  } else {
    logs.push({ time: `00:${fmt(t++)}`, phase: 'semantic', level: 'ok',   msg: 'Validación semántica exitosa' });
  }
  if (result.warnings?.length) {
    result.warnings.forEach(w => {
      logs.push({ time: `00:${fmt(t++)}`, phase: 'semantic', level: 'warn', msg: w });
    });
  }

  logs.push({ time: `00:${fmt(t++)}`, phase: 'optimizer', level: 'info',  msg: 'Aplicando reglas de optimización...' });
  const sug = result.suggestions?.length || 0;
  logs.push({ time: `00:${fmt(t++)}`, phase: 'optimizer', level: 'ok',    msg: `${sug} sugerencia(s) de optimización generadas` });

  logs.push({
    time: `00:${fmt(t++)}`,
    phase: 'system',
    level: result.valid ? 'ok' : 'error',
    msg:   result.valid ? 'Compilación completada — ÉXITO' : 'Compilación completada — CON ERRORES',
  });

  return logs;
}

const PHASE_LABELS = {
  lexer: 'LÉXICO', parser: 'SINTÁCT', semantic: 'SEMÁNT', optimizer: 'OPTIM', system: 'SISTEMA',
};

function CompilerLogTab({ result }) {
  if (!result) return <p className="empty-msg">Ejecuta una consulta para ver el log.</p>;
  const logs = buildLog(result);
  return (
    <div className="log-terminal">
      {logs.map((entry, i) => (
        <div key={i} className={`log-line log-${entry.level}`}>
          <span className="log-time">[{entry.time}]</span>
          <span className={`log-tag lt-${entry.phase}`}>{PHASE_LABELS[entry.phase]}</span>
          <span className="log-msg">{entry.msg}</span>
        </div>
      ))}
    </div>
  );
}

// ── Tabla de Símbolos ────────────────────────────────────────────────────────

function SymbolTableTab({ schema }) {
  if (!schema?.length) return <p className="empty-msg">Schema no disponible.</p>;
  return (
    <div className="symtable">
      <p className="symtable-hint">Tabla de Símbolos — tipos registrados en el schema de la BD:</p>
      {schema.map((table, i) => (
        <div key={i} className="symtable-block">
          <div className="symtable-header">
            <span className="sym-kw">TABLE</span>
            <span className="sym-tname">{table.name}</span>
            <span className="sym-count">{table.columns.length} cols</span>
          </div>
          <div className="symtable-cols">
            {table.columns.map((col, j) => {
              const isLast = j === table.columns.length - 1;
              return (
                <div key={j} className="sym-row">
                  <span className="sym-tree">{isLast ? '└──' : '├──'}</span>
                  <span className="sym-col-name">{col.name}</span>
                  <span className={`sym-type st-${col.type.toLowerCase()}`}>{col.type}</span>
                  {col.primaryKey && <span className="sym-pk">PK</span>}
                </div>
              );
            })}
          </div>
        </div>
      ))}
    </div>
  );
}
