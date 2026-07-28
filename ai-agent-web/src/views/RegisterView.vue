<template>
  <div class="register-page">
    <div class="register-box">
      <div class="register-header">
        <h1>AI 智能应用中心</h1>
        <p>创建新账号</p>
      </div>

      <form class="register-form" @submit.prevent="handleRegister">
        <div class="form-item">
          <label>邮箱地址</label>
          <div class="email-row">
            <input
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱"
              required
              @blur="checkEmailExists"
            />
            <button
              type="button"
              class="code-btn"
              :disabled="codeCountdown > 0 || checkingEmail"
              @click="sendCode"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s后重发` : '获取验证码' }}
            </button>
          </div>
          <span v-if="emailCheckMsg" :class="['check-msg', emailCheckError ? 'error' : 'success']">
            {{ emailCheckMsg }}
          </span>
        </div>

        <div class="form-item">
          <label>验证码</label>
          <input
            v-model="form.verifyCode"
            type="text"
            placeholder="请输入邮箱验证码"
            maxlength="6"
            required
          />
        </div>

        <div class="form-item">
          <label>昵称</label>
          <input
            v-model="form.nickname"
            type="text"
            placeholder="请输入昵称"
            required
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="8-20位，需包含数字、大小写字母"
            required
          />
          <span class="hint">密码长度8-20位，必须包含数字、小写字母、大写字母</span>
        </div>

        <div class="form-item">
          <label>确认密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            required
          />
        </div>

        <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>

        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { sendVerifyCode, register, checkEmail } from '../api/auth';

const router = useRouter();
const loading = ref(false);
const errorMsg = ref('');
const codeCountdown = ref(0);
const checkingEmail = ref(false);
const emailCheckMsg = ref('');
const emailCheckError = ref(false);

const form = reactive({
  email: '',
  verifyCode: '',
  nickname: '',
  password: '',
  confirmPassword: ''
});

let countdownTimer: ReturnType<typeof setInterval> | null = null;

/**
 * 将后端返回的技术错误信息转换为用户友好提示
 */
function toUserMessage(raw: string, fallback: string): string {
  if (!raw) return fallback;
  if (raw.includes('CannotGetJdbcConnectionException') || raw.includes('JDBC Connection')) {
    return '服务暂时不可用，请稍后重试';
  }
  if (raw.includes('timeout') || raw.includes('Timeout')) {
    return '请求超时，请检查网络后重试';
  }
  return raw;
}

function startCountdown() {
  codeCountdown.value = 60;
  countdownTimer = setInterval(() => {
    codeCountdown.value--;
    if (codeCountdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer);
    }
  }, 1000);
}

async function checkEmailExists() {
  if (!form.email) return;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(form.email)) {
    emailCheckMsg.value = '邮箱格式不正确';
    emailCheckError.value = true;
    return;
  }

  checkingEmail.value = true;
  try {
    const res = await checkEmail(form.email);
    if (res.data.data.registered) {
      emailCheckMsg.value = '该邮箱已被注册';
      emailCheckError.value = true;
    } else {
      emailCheckMsg.value = '该邮箱可用';
      emailCheckError.value = false;
    }
  } catch {
    emailCheckMsg.value = '';
  } finally {
    checkingEmail.value = false;
  }
}

async function sendCode() {
  if (!form.email) {
    errorMsg.value = '请先输入邮箱地址';
    return;
  }

  errorMsg.value = '';

  try {
    const res = await sendVerifyCode(form.email);
    if (res.data.code === 200) {
      startCountdown();
    } else {
      errorMsg.value = res.data.message || '发送失败';
    }
  } catch (err: unknown) {
    const error = err as { response?: { data?: { message?: string }; status?: number } };
    const rawMsg = error.response?.data?.message || '';
    errorMsg.value = toUserMessage(rawMsg, '发送失败，请稍后重试');
  }
}

async function handleRegister() {
  // 前端校验
  if (!form.email || !form.verifyCode || !form.nickname || !form.password || !form.confirmPassword) {
    errorMsg.value = '请填写完整信息';
    return;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(form.email)) {
    errorMsg.value = '邮箱格式不正确';
    return;
  }

  if (form.password.length < 8 || form.password.length > 20) {
    errorMsg.value = '密码长度需8-20位';
    return;
  }

  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,20}$/;
  if (!passwordRegex.test(form.password)) {
    errorMsg.value = '密码必须包含数字、小写字母、大写字母';
    return;
  }

  if (form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致';
    return;
  }

  loading.value = true;
  errorMsg.value = '';

  try {
    const res = await register({
      email: form.email,
      password: form.password,
      confirmPassword: form.confirmPassword,
      verifyCode: form.verifyCode,
      nickname: form.nickname
    });
    if (res.data.code === 200) {
      alert('注册成功，请登录');
      router.push('/login');
    } else {
      errorMsg.value = res.data.message || '注册失败';
    }
  } catch (err: unknown) {
    const error = err as { response?: { data?: { message?: string }; status?: number } };
    const rawMsg = error.response?.data?.message || '';
    errorMsg.value = toUserMessage(rawMsg, '网络错误，请稍后重试');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 20px;
}

.register-box {
  width: 460px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.register-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px 0;
}

.register-header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.email-row {
  display: flex;
  gap: 8px;
}

.email-row input {
  flex: 1;
}

.code-btn {
  padding: 0 16px;
  background: #4a90e2;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.code-btn:hover:not(:disabled) {
  background: #357abd;
}

.code-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.hint {
  font-size: 12px;
  color: #999;
}

.check-msg {
  font-size: 12px;
}

.check-msg.error {
  color: #e74c3c;
}

.check-msg.success {
  color: #27ae60;
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
