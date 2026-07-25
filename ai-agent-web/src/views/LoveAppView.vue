<template>
  <div class="love-app">
    <div class="nav-bar">
      <button class="back-btn" @click="goBack">← 返回首页</button>
      <span class="nav-title">AI 恋爱大师</span>
    </div>
    <ChatRoom
      title="AI 恋爱大师"
      :chat-id="chatId"
      :on-send-message="handleSendMessage"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import ChatRoom from '../components/ChatRoom.vue';
import { generateChatId, doChatWithLoveAppSse } from '../api/chat';

const router = useRouter();
const chatId = ref('');

onMounted(() => {
  chatId.value = generateChatId();
});

function goBack() {
  router.push('/');
}

function handleSendMessage(message: string, chatId?: string) {
  const eventSource = doChatWithLoveAppSse(
    message,
    chatId || '',
    () => {}, // onMessage 在组件内部处理
    () => {}, // onError
    () => {}  // onComplete
  );

  return {
    eventSource,
    close: () => eventSource.close()
  };
}
</script>

<style scoped>
.love-app {
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
