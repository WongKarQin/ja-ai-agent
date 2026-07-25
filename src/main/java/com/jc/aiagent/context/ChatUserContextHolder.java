package com.jc.aiagent.context;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天会话用户上下文持有器
 * 用于在异步线程（Flux / CompletableFuture）中传递 userId
 * 通过 chatId -> userId 映射解决 ThreadLocal 跨线程丢失问题
 */
public class ChatUserContextHolder {

    private static final ConcurrentHashMap<String, Long> CHAT_USER_MAP = new ConcurrentHashMap<>();

    /**
     * 绑定 chatId 与 userId
     */
    public static void bind(String chatId, Long userId) {
        if (chatId != null && userId != null) {
            CHAT_USER_MAP.put(chatId, userId);
        }
    }

    /**
     * 根据 chatId 获取 userId
     */
    public static Long getUserId(String chatId) {
        if (chatId == null) {
            return null;
        }
        return CHAT_USER_MAP.get(chatId);
    }

    /**
     * 解绑 chatId
     */
    public static void unbind(String chatId) {
        if (chatId != null) {
            CHAT_USER_MAP.remove(chatId);
        }
    }
}
