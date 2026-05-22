import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/common/PrivateRoute';
import Navbar    from './components/common/Navbar';
import Login     from './components/auth/Login';
import Register  from './components/auth/Register';
import Dashboard          from './pages/Dashboard';
import ProfessorDashboard from './pages/ProfessorDashboard';
import Editor             from './pages/Editor';
import './App.css';

function Layout({ children }) {
  return (
    <>
      <Navbar />
      {children}
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login"    element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route path="/editor" element={
            <PrivateRoute>
              <Layout><Editor /></Layout>
            </PrivateRoute>
          } />

          <Route path="/dashboard" element={
            <PrivateRoute>
              <Layout><Dashboard /></Layout>
            </PrivateRoute>
          } />

          <Route path="/dashboard/global" element={
            <PrivateRoute>
              <Layout><ProfessorDashboard /></Layout>
            </PrivateRoute>
          } />

          <Route path="*" element={<Navigate to="/editor" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
