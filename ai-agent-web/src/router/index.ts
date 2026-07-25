import { createRouter, createWebHistory } from 'vue-router';
import { isLoggedIn } from '../api/auth';
import HomeView from '../views/HomeView.vue';
import LoveAppView from '../views/LoveAppView.vue';
import ManusAppView from '../views/ManusAppView.vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView,
    meta: { requiresAuth: true }
  },
  {
    path: '/love-app',
    name: 'LoveApp',
    component: LoveAppView,
    meta: { requiresAuth: true }
  },
  {
    path: '/manus',
    name: 'Manus',
    component: ManusAppView,
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView,
    meta: { guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterView,
    meta: { guestOnly: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 路由守卫
router.beforeEach((to, _from, next) => {
  const loggedIn = isLoggedIn();

  // 需要登录的页面
  if (to.meta.requiresAuth && !loggedIn) {
    next('/login');
    return;
  }

  // 仅限未登录用户访问的页面（登录/注册页）
  if (to.meta.guestOnly && loggedIn) {
    next('/');
    return;
  }

  next();
});

export default router;
