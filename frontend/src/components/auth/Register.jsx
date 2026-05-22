import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import './Login.css';

export default function Register() {
  const { login } = useAuth();
  const navigate  = useNavigate();

  const [form,    setForm]    = useState({ nombre: '', apellido: '', email: '', password: '' });
  const [error,   setError]   = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');
    if (form.password.length < 8) { setError('La contraseña debe tener al menos 8 caracteres'); return; }
    if (!/[A-Z]/.test(form.password)) { setError('La contraseña debe tener al menos una mayúscula'); return; }
    if (!/[0-9]/.test(form.password)) { setError('La contraseña debe tener al menos un número'); return; }
    setLoading(true);
    try {
      const { data } = await api.post('/auth/register', form);
      login(data.token, {
        id:       data.userId,
        nombre:   data.nombre,
        apellido: data.apellido,
        email:    data.email,
        role:     data.role,
      });
      navigate('/editor');
    } catch (err) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Error al registrarse');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo-text">DQ</div>
        <h1 className="auth-title">DataQuery</h1>
        <p className="auth-subtitle">Crea tu cuenta gratuita</p>

        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label>Nombre</label>
            <input
              type="text" name="nombre" required
              value={form.nombre} onChange={handleChange}
              placeholder="Juan"
            />
          </div>
          <div className="form-group">
            <label>Apellido</label>
            <input
              type="text" name="apellido" required
              value={form.apellido} onChange={handleChange}
              placeholder="García"
            />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input
              type="email" name="email" required
              value={form.email} onChange={handleChange}
              placeholder="tu@email.com"
            />
          </div>
          <div className="form-group">
            <label>Contraseña</label>
            <input
              type="password" name="password" required minLength={8}
              value={form.password} onChange={handleChange}
              placeholder="Mínimo 6 caracteres"
            />
          </div>
          <button type="submit" className="auth-btn" disabled={loading}>
            {loading ? 'Creando cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <p className="auth-link">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </div>
    </div>
  );
}
