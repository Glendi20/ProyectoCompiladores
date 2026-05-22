import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function PrivateRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center',
                  height:'100vh', background:'#0f0f1a', color:'#6366f1', fontSize:'1.2rem' }}>
      Cargando...
    </div>
  );

  return user ? children : <Navigate to="/login" replace />;
}
