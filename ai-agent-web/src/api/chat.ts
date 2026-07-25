import { getToken } from './auth';

/**
 * 获取API基础路径（兼容开发和生产环境）
 */
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * 生成唯一聊天室ID
 */
export function generateChatId(): string {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

/**
 * 调用AI恋爱大师SSE接口
 */
export function doChatWithLoveAppSse(
  message: string,
  chatId: string,
  onMessage: (data: string) => void,
  onError?: (error: Event) => void,
  onComplete?: () => void
): EventSource {
  const encodedMessage = encodeURIComponent(message);
  const token = getToken() || '';
  const url = `${API_BASE}/ai/love_app/chat/sse_emitter?message=${encodedMessage}&chatId=${chatId}&token=${token}`;

  const eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    if (event.data) {
      onMessage(event.data);
    }
  };

  eventSource.onerror = (error) => {
    if (onError) {
      onError(error);
    }
    eventSource.close();
    if (onComplete) {
      onComplete();
    }
  };

  eventSource.addEventListener('complete', () => {
    eventSource.close();
    if (onComplete) {
      onComplete();
    }
  });

  return eventSource;
}

/**
 * 调用AI超级智能体SSE接口
 */
export function doChatWithManus(
  message: string,
  onMessage: (data: string) => void,
  onError?: (error: Event) => void,
  onComplete?: () => void
): EventSource {
  const encodedMessage = encodeURIComponent(message);
  const token = getToken() || '';
  const url = `${API_BASE}/ai/manus/chat?message=${encodedMessage}&token=${token}`;

  const eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    if (event.data) {
      onMessage(event.data);
    }
  };

  eventSource.onerror = (error) => {
    if (onError) {
      onError(error);
    }
    eventSource.close();
    if (onComplete) {
      onComplete();
    }
  };

  eventSource.addEventListener('complete', () => {
    eventSource.close();
    if (onComplete) {
      onComplete();
    }
  });

  return eventSource;
}
