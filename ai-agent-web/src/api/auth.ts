import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器：自动附加Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：统一处理401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

/**
 * 发送注册验证码
 */
export function sendVerifyCode(email: string) {
  return api.post('/user/send-verify-code', { email });
}

/**
 * 用户注册
 */
export function register(data: {
  email: string;
  password: string;
  confirmPassword: string;
  verifyCode: string;
  nickname: string;
}) {
  return api.post('/user/register', data);
}

/**
 * 用户登录
 */
export function login(email: string, password: string) {
  return api.post('/user/login', { email, password });
}

/**
 * 检查邮箱是否已注册
 */
export function checkEmail(email: string) {
  return api.get('/user/check-email', { params: { email } });
}

/**
 * 保存登录状态
 */
export function saveAuth(token: string, email: string) {
  localStorage.setItem('token', token);
  localStorage.setItem('userInfo', JSON.stringify({ email }));
}

/**
 * 获取Token
 */
export function getToken(): string | null {
  return localStorage.getItem('token');
}

/**
 * 判断是否已登录
 */
export function isLoggedIn(): boolean {
  return !!getToken();
}

/**
 * 清除登录状态
 */
export function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('userInfo');
}

/**
 * 获取用户信息
 */
export function getUserInfo(): { email: string } | null {
  const info = localStorage.getItem('userInfo');
  return info ? JSON.parse(info) : null;
}

export default api;
