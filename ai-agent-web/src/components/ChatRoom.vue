<template>
  <div class="chat-room">
    <div class="chat-header">
      <h2>{{ title }}</h2>
      <span class="chat-id" v-if="chatId">会话ID: {{ chatId }}</span>
    </div>
    
    <div class="chat-messages" ref="messagesContainer">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.role === 'user' ? 'message-user' : 'message-ai']"
      >
        <div class="message-avatar">
          {{ msg.role === 'user' ? '我' : 'AI' }}
        </div>
        <div class="message-content">
          <div class="message-text">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>
      
      <div v-if="isLoading" class="message message-ai loading">
        <div class="message-avatar">AI</div>
        <div class="message-content">
          <!-- 思考状态标签 -->
          <div v-if="thinkingSteps.length > 0" class="thinking-tags">
            <span
              v-for="(step, i) in thinkingSteps"
              :key="i"
              class="thinking-tag"
            >{{ step }}</span>
          </div>
          <!-- 默认加载提示（无思考状态时显示） -->
          <div v-if="thinkingSteps.length === 0" class="message-text">
            <span class="loading-dots">思考中</span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="chat-input-area">
      <div class="input-wrapper">
        <textarea
          v-model="inputMessage"
          placeholder="请输入消息..."
          @keydown.enter.prevent="handleSend"
          :disabled="isLoading"
          rows="1"
        ></textarea>
        <button
          @click="handleSend"
          :disabled="isLoading || !inputMessage.trim()"
          class="send-btn"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';

interface Message {
  role: 'user' | 'ai';
  content: string;
  time: string;
}

interface Props {
  title: string;
  chatId?: string;
  onSendMessage: (message: string, chatId?: string) => {
    eventSource: EventSource;
    close: () => void;
  };
}

const props = defineProps<Props>();

const messages = ref<Message[]>([]);
const inputMessage = ref('');
const isLoading = ref(false);
const messagesContainer = ref<HTMLDivElement>();
const thinkingSteps = ref<string[]>([]);
let currentEventSource: EventSource | null = null;

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
}

function getCurrentTime(): string {
  const now = new Date();
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
}

function handleSend() {
  const message = inputMessage.value.trim();
  if (!message || isLoading.value) return;

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: message,
    time: getCurrentTime()
  });
  
  inputMessage.value = '';
  isLoading.value = true;
  thinkingSteps.value = [];
  scrollToBottom();

  // 调用SSE接口
  const result = props.onSendMessage(message, props.chatId);
  currentEventSource = result.eventSource;

  let aiContent = '';
  
  // 默认事件处理（兼容 LoveApp 的未命名 SSE 事件）
  currentEventSource.onmessage = (event) => {
    if (event.data) {
      aiContent += event.data;
      // 更新最后一条AI消息或创建新消息
      const lastMsg = messages.value[messages.value.length - 1];
      if (lastMsg && lastMsg.role === 'ai') {
        lastMsg.content = aiContent;
      } else {
        messages.value.push({
          role: 'ai',
          content: aiContent,
          time: getCurrentTime()
        });
      }
      scrollToBottom();
    }
  };

  // 思考状态事件（Manus 智能体精简展示）
  currentEventSource.addEventListener('thinking', (event: MessageEvent) => {
    if (event.data) {
      thinkingSteps.value.push(event.data);
      scrollToBottom();
    }
  });

  // 最终答案事件
  currentEventSource.addEventListener('answer', (event: MessageEvent) => {
    if (event.data) {
      aiContent = event.data;
      messages.value.push({
        role: 'ai',
        content: aiContent,
        time: getCurrentTime()
      });
      // 收到答案后清空思考状态
      thinkingSteps.value = [];
      scrollToBottom();
    }
  });

  // 错误事件
  currentEventSource.addEventListener('error', (event: MessageEvent) => {
    if (event.data) {
      messages.value.push({
        role: 'ai',
        content: '⚠️ ' + event.data,
        time: getCurrentTime()
      });
    }
    isLoading.value = false;
    thinkingSteps.value = [];
    if (currentEventSource) {
      currentEventSource.close();
      currentEventSource = null;
    }
    scrollToBottom();
  });

  // 完成事件
  currentEventSource.addEventListener('complete', () => {
    isLoading.value = false;
    thinkingSteps.value = [];
    if (currentEventSource) {
      currentEventSource.close();
      currentEventSource = null;
    }
  });

  // 连接错误兜底处理
  currentEventSource.onerror = () => {
    // 仅在没有收到任何AI内容时显示错误提示
    if (aiContent === '' && thinkingSteps.value.length === 0) {
      messages.value.push({
        role: 'ai',
        content: '⚠️ 连接异常，请重试',
        time: getCurrentTime()
      });
    }
    isLoading.value = false;
    thinkingSteps.value = [];
    if (currentEventSource) {
      currentEventSource.close();
      currentEventSource = null;
    }
    scrollToBottom();
  };
}

onMounted(() => {
  // 添加欢迎消息
  messages.value.push({
    role: 'ai',
    content: '你好！我是你的AI助手，有什么可以帮助你的吗？',
    time: getCurrentTime()
  });
});
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.chat-header h2 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

.chat-id {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 4px 8px;
  border-radius: 4px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 80%;
}

.message-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-ai {
  align-self: flex-start;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #4a90e2;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.message-user .message-avatar {
  background: #67c23a;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-text {
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  color: #333;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-user .message-text {
  background: #4a90e2;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #999;
  padding: 0 4px;
}

.message-user .message-time {
  text-align: right;
}

/* 思考状态标签样式 */
.thinking-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px 0;
}

.thinking-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e8f0fe 0%, #f0e6ff 100%);
  color: #555;
  font-size: 12px;
  white-space: nowrap;
  animation: tagPulse 1.5s ease-in-out infinite;
  border: 1px solid rgba(74, 144, 226, 0.15);
}

@keyframes tagPulse {
  0%, 100% {
    opacity: 0.85;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.03);
  }
}

.loading .message-text {
  background: #f0f0f0;
}

.loading-dots::after {
  content: '';
  animation: dots 1.5s infinite;
}

@keyframes dots {
  0%, 20% { content: '.'; }
  40% { content: '..'; }
  60%, 100% { content: '...'; }
}

.chat-input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e0e0e0;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-wrapper textarea {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #dcdcdc;
  border-radius: 8px;
  resize: none;
  font-size: 14px;
  line-height: 1.5;
  outline: none;
  transition: border-color 0.2s;
  min-height: 44px;
  max-height: 120px;
}

.input-wrapper textarea:focus {
  border-color: #4a90e2;
}

.input-wrapper textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.send-btn {
  padding: 12px 24px;
  background: #4a90e2;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
  white-space: nowrap;
}

.send-btn:hover:not(:disabled) {
  background: #357abd;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
