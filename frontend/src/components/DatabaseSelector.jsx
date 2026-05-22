import React from 'react';
import './DatabaseSelector.css';

const ICONS = { mysql: '🐬', postgresql: '🐘', sqlserver: '🏢', mongodb: '🍃' };

function DatabaseSelector({ databases, selected, onChange }) {
  return (
    <div className="database-selector">
      <h3>Base de Datos</h3>
      <p className="selector-hint">Elige el motor para ver sugerencias específicas</p>
      <div className="db-list">
        {databases.map(db => (
          <button
            key={db.id}
            className={`db-btn ${selected === db.id ? 'active' : ''}`}
            onClick={() => onChange(db.id)}
            title={db.description}
          >
            <span className="db-icon">{db.icon || ICONS[db.id]}</span>
            <span className="db-name">{db.name}</span>
            {selected === db.id && <span className="db-check">✓</span>}
          </button>
        ))}
      </div>
    </div>
  );
}

export default DatabaseSelector;
