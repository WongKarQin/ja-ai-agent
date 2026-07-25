<template>
  <div class="manus-app">
    <div class="nav-bar">
      <button class="back-btn" @click="goBack">← 返回首页</button>
      <span class="nav-title">AI 超级智能体</span>
    </div>
    <ChatRoom
      title="AI 超级智能体"
      :on-send-message="handleSendMessage"
    />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable no-unused-vars */
import { useRouter } from 'vue-router';
import ChatRoom from '../components/ChatRoom.vue';
import { doChatWithManus } from '../api/chat';

const router = useRouter();

function goBack() {
  router.push('/');
}

function handleSendMessage(message: string) {
  const eventSource = doChatWithManus(
    message,
    () => {}, // onMessage 在组件内部处理
    () => {}, // onError
    () => {}, // onComplete
  );

  return {
    eventSource,
    close: () => eventSource.close(),
  };
}
</script>

<style scoped>
.manus-app {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.nav-bar {
  display: flex;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  gap: 16px;
}

.back-btn {
  padding: 8px 16px;
  background: #f5f7fa;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #e8ecf1;
  color: #333;
}

.nav-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}
</style>
