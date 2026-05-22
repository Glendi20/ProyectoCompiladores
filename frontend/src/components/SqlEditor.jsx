import React from 'react';
import Editor from '@monaco-editor/react';
import './SqlEditor.css';

const EXAMPLES = {
  mysql:      'SELECT id, nombre, email\nFROM usuarios\nWHERE edad > 18\nORDER BY nombre',
  postgresql: "SELECT id, nombre\nFROM usuarios\nWHERE ciudad = 'Guatemala'",
  sqlserver:  'SELECT id, nombre, precio\nFROM productos\nWHERE precio < 100',
  mongodb:    "SELECT nombre, precio\nFROM productos\nWHERE categoria = 'electronica'",
};

const MONACO_OPTIONS = {
  fontSize: 14,
  minimap: { enabled: false },
  scrollBeyondLastLine: false,
  wordWrap: 'on',
  lineNumbers: 'on',
  renderLineHighlight: 'line',
  theme: 'vs-dark',
  automaticLayout: true,
  padding: { top: 12 },
  fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
};

function SqlEditor({ query, onChange, onCompile, loading, selectedDb }) {
  const handleMount = (editor) => {
    // Ctrl+Enter para compilar desde Monaco
    editor.addCommand(
      // Monaco usa KeyMod.CtrlCmd | KeyCode.Enter
      2048 | 3,  // CtrlCmd=2048, Enter=3
      () => onCompile()
    );
  };

  return (
    <div className="sql-editor">
      <div className="editor-header">
        <h3>Editor SQL</h3>
        <div className="editor-actions">
          <span className="editor-hint"><kbd>Ctrl</kbd>+<kbd>Enter</kbd> para analizar</span>
          <button className="example-btn" onClick={() => onChange(EXAMPLES[selectedDb] || EXAMPLES.mysql)}>
            Cargar ejemplo
          </button>
        </div>
      </div>

      <div className="monaco-wrapper">
        <Editor
          height="100%"
          language="sql"
          value={query}
          onChange={val => onChange(val ?? '')}
          onMount={handleMount}
          options={MONACO_OPTIONS}
          theme="vs-dark"
        />
      </div>

      <button className={`compile-btn ${loading ? 'loading' : ''}`} onClick={onCompile} disabled={loading}>
        {loading ? '⏳ Analizando...' : '▶  Analizar Consulta'}
      </button>
    </div>
  );
}

export default SqlEditor;
