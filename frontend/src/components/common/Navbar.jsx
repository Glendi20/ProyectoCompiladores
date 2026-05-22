import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './Navbar.css';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate         = useNavigate();
  const location         = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = path => location.pathname === path ? 'nav-link active' : 'nav-link';

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <span className="navbar-logo">⚡</span>
        <span className="navbar-title">DataQuery</span>
      </div>

      <div className="navbar-links">
        <Link to="/editor"    className={isActive('/editor')}>Editor SQL</Link>
        <Link to="/dashboard" className={isActive('/dashboard')}>Dashboard</Link>
        {user?.role === 'PROFESSOR' && (
          <Link to="/dashboard/global" className={isActive('/dashboard/global')}>
            Vista Admin
          </Link>
        )}
      </div>

      <div className="navbar-user">
        <span className="navbar-name">{user?.nombre} {user?.apellido}</span>
        <span className={`navbar-role ${user?.role === 'PROFESSOR' ? 'role-prof' : 'role-student'}`}>
          {user?.role === 'PROFESSOR' ? 'Administrador' : 'Estudiante'}
        </span>
        <button onClick={handleLogout} className="navbar-logout">Salir</button>
      </div>
    </nav>
  );
}
