import React, { useState } from 'react';
import './SchemaPanel.css';

export default function SchemaPanel({ schema }) {
  const [open, setOpen] = useState(true);
  if (!schema?.length) return null;

  return (
    <div className="schema-panel">
      <button className="schema-toggle" onClick={() => setOpen(!open)}>
        <span>🗄️ Schema de BD</span>
        <span>{open ? '▲' : '▼'}</span>
      </button>

      {open && (
        <div className="schema-scroll">
          <p className="schema-hint">Tablas disponibles para consultar:</p>
          {schema.map((table, i) => (
            <div key={i} className="schema-table">
              <div className="schema-table-name">
                <span>📁 {table.name}</span>
                <span className="schema-col-count">{table.columns.length}</span>
              </div>
              <div className="schema-columns">
                {table.columns.map((col, j) => (
                  <div key={j} className="schema-column">
                    <span className="col-name">{col.name}</span>
                    <span className={`col-type type-${col.type.toLowerCase()}`}>{col.type}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
