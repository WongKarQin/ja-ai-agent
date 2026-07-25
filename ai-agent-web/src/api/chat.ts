import { getToken } from './auth';

/**
 * 生成唯一聊天室ID
 */
export function generateChatId(): string {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

/**
 * 调用AI恋爱大师SSE接口
 * @param message 用户消息
 * @param chatId 聊天室ID
 * @param onMessage 收到消息回调
 * @param onError 错误回调
 * @param onComplete 完成回调
 * @returns EventSource实例
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
  const url = `http://localhost:8123/api/ai/love_app/chat/sse_emitter?message=${encodedMessage}&chatId=${chatId}&token=${token}`;

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
 * @param message 用户消息
 * @param onMessage 收到消息回调
 * @param onError 错误回调
 * @param onComplete 完成回调
 * @returns EventSource实例
 */
export function doChatWithManus(
  message: string,
  onMessage: (data: string) => void,
  onError?: (error: Event) => void,
  onComplete?: () => void
): EventSource {
  const encodedMessage = encodeURIComponent(message);
  const token = getToken() || '';
  const url = `http://localhost:8123/api/ai/manus/chat?message=${encodedMessage}&token=${token}`;

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
