<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-header">
        <h1>AI 智能应用中心</h1>
        <p>欢迎回来，请登录</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-item">
          <label>邮箱地址</label>
          <input
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            required
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { login, saveAuth } from '../api/auth';

const router = useRouter();
const loading = ref(false);
const errorMsg = ref('');

const form = reactive({
  email: '',
  password: ''
});

async function handleLogin() {
  if (!form.email || !form.password) {
    errorMsg.value = '请填写完整信息';
    return;
  }

  loading.value = true;
  errorMsg.value = '';

  try {
    const res = await login(form.email, form.password);
    if (res.data.code === 200) {
      const token = res.data.data.token;
      saveAuth(token, form.email);
      router.push('/');
    } else {
      errorMsg.value = res.data.message || '登录失败';
    }
  } catch (err: unknown) {
    const error = err as { response?: { data?: { message?: string } } };
    errorMsg.value = error.response?.data?.message || '网络错误，请稍后重试';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-box {
  width: 420px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px 0;
}

.login-header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-item input {
  padding: 12px 16px;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.form-item input:focus {
  border-color: #4a90e2;
}

.error-msg {
  color: #e74c3c;
  font-size: 13px;
  text-align: center;
}

.submit-btn {
  padding: 14px;
  background: #4a90e2;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: #357abd;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.form-footer a {
  color: #4a90e2;
  text-decoration: none;
  margin-left: 4px;
}

.form-footer a:hover {
  text-decoration: underline;
}
</style>
