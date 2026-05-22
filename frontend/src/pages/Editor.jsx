import React, { useState, useEffect } from 'react';
import api from '../services/api';
import DatabaseSelector from '../components/DatabaseSelector';
import SqlEditor        from '../components/SqlEditor';
import ResultPanel      from '../components/ResultPanel';
import SchemaPanel      from '../components/SchemaPanel';
import PipelineVisual   from '../components/PipelineVisual';
import './Editor.css';

export default function Editor() {
  const [selectedDb, setSelectedDb] = useState('mysql');
  const [query,      setQuery]      = useState('SELECT id, nombre, email FROM usuarios WHERE activo = 1');
  const [result,     setResult]     = useState(null);
  const [loading,    setLoading]    = useState(false);
  const [databases,  setDatabases]  = useState([]);
  const [schema,     setSchema]     = useState([]);

  useEffect(() => {
    api.get('/databases').then(r => setDatabases(r.data)).catch(() => {});
    api.get('/schema').then(r => setSchema(r.data)).catch(() => {});
  }, []);

  const handleCompile = async () => {
    if (!query.trim()) return;
    setLoading(true);
    setResult(null);
    try {
      const { data } = await api.post('/compile', { query, database: selectedDb });
      setResult(data);
    } catch {
      setResult({
        valid: false,
        errors: ['No se pudo conectar al servidor. Asegúrate de que el backend esté corriendo.'],
        tokens: [], suggestions: [],
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="editor-page">
      <div className="left-panel">
        <DatabaseSelector databases={databases} selected={selectedDb} onChange={setSelectedDb} />
        <SchemaPanel schema={schema} />
      </div>

      <div className="center-panel">
        <PipelineVisual result={result} loading={loading} />
        <SqlEditor
          query={query}
          onChange={setQuery}
          onCompile={handleCompile}
          loading={loading}
          selectedDb={selectedDb}
        />
      </div>

      <div className="right-panel">
        <ResultPanel result={result} loading={loading} schema={schema} />
      </div>
    </div>
  );
}
