<template>
  <div class="home">
    <div class="home-header-bar">
      <h1>AI 智能应用中心</h1>
      <div class="user-info" v-if="userInfo">
        <span class="user-email">{{ userInfo.email }}</span>
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </div>

    <div class="home-content">
      <p class="subtitle">选择下方的应用开始体验</p>

      <div class="app-cards">
        <div class="app-card" @click="goToLoveApp">
          <div class="card-icon love-icon">❤️</div>
          <h2>AI 恋爱大师</h2>
          <p>专业的恋爱咨询助手，为你解答感情问题，提供恋爱建议</p>
          <div class="card-footer">
            <span class="tag">恋爱咨询</span>
            <span class="arrow">→</span>
          </div>
        </div>

        <div class="app-card" @click="goToManus">
          <div class="card-icon manus-icon">🤖</div>
          <h2>AI 超级智能体</h2>
          <p>强大的AI智能体，支持多种工具调用，帮你完成复杂任务</p>
          <div class="card-footer">
            <span class="tag">智能助手</span>
            <span class="arrow">→</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo, clearAuth } from '../api/auth';

const router = useRouter();
const userInfo = ref<{ email: string } | null>(null);

onMounted(() => {
  userInfo.value = getUserInfo();
});

function goToLoveApp() {
  router.push('/love-app');
}

function goToManus() {
  router.push('/manus');
}

function handleLogout() {
  clearAuth();
  router.push('/login');
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: #f5f7fa;
}

.home-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 40px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.home-header-bar h1 {
  font-size: 20px;
  color: #333;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-email {
  font-size: 14px;
  color: #666;
}

.logout-btn {
  padding: 6px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #e8e8e8;
  color: #333;
}

.home-content {
  padding: 40px 20px;
}

.subtitle {
  text-align: center;
  font-size: 16px;
  color: #666;
  margin: 0 0 48px 0;
}

.app-cards {
  display: flex;
  justify-content: center;
  gap: 32px;
  max-width: 900px;
  margin: 0 auto;
  flex-wrap: wrap;
}

.app-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  width: 380px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid #f0f0f0;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin-bottom: 20px;
}

.love-icon {
  background: #fff0f0;
}

.manus-icon {
  background: #f0f5ff;
}

.app-card h2 {
  font-size: 22px;
  color: #333;
  margin: 0 0 12px 0;
}

.app-card p {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag {
  padding: 6px 12px;
  background: #f5f7fa;
  color: #666;
  font-size: 12px;
  border-radius: 20px;
}

.arrow {
  font-size: 20px;
  color: #4a90e2;
  transition: transform 0.2s;
}

.app-card:hover .arrow {
  transform: translateX(4px);
}
</style>
