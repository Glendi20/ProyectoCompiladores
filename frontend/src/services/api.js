import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

// Inyectar el JWT en cada petición automáticamente
api.interceptors.request.use(config => {
  const token = localStorage.getItem('dq_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Si el servidor devuelve 401, limpiar sesión y redirigir al login
api.interceptors.response.use(
  res => res,
  err => {
    const isLoginRequest = err.config?.url?.includes('/auth/login');
    if (err.response?.status === 401 && !isLoginRequest) {
      localStorage.removeItem('dq_token');
      localStorage.removeItem('dq_user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default api;
