package com.jc.aiagent.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码缓存（内存实现，生产环境建议改用Redis）
 */
public class VerifyCodeCache {

    /**
     * 验证码缓存: key=邮箱, value=验证码
     */
    private static final Map<String, CodeEntry> CACHE = new ConcurrentHashMap<>();

    /**
     * 验证码有效期: 5分钟 (单位: 毫秒)
     */
    private static final long EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * 同一邮箱发送间隔: 60秒
     */
    private static final long SEND_INTERVAL = 60 * 1000;

    private static class CodeEntry {
        String code;
        long createTime;

        CodeEntry(String code, long createTime) {
            this.code = code;
            this.createTime = createTime;
        }
    }

    /**
     * 存储验证码
     */
    public static void put(String email, String code) {
        cleanExpired();
        CACHE.put(email, new CodeEntry(code, System.currentTimeMillis()));
    }

    /**
     * 获取验证码
     */
    public static String get(String email) {
        cleanExpired();
        CodeEntry entry = CACHE.get(email);
        return entry != null ? entry.code : null;
    }

    /**
     * 删除验证码
     */
    public static void remove(String email) {
        CACHE.remove(email);
    }

    /**
     * 验证验证码是否正确
     */
    public static boolean verify(String email, String code) {
        if (code == null || email == null) {
            return false;
        }
        String cachedCode = get(email);
        return code.equalsIgnoreCase(cachedCode);
    }

    /**
     * 检查是否可以发送验证码（间隔限制）
     */
    public static boolean canSend(String email) {
        cleanExpired();
        CodeEntry entry = CACHE.get(email);
        if (entry == null) {
            return true;
        }
        return System.currentTimeMillis() - entry.createTime > SEND_INTERVAL;
    }

    /**
     * 清理过期验证码
     */
    private static void cleanExpired() {
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(entry -> now - entry.getValue().createTime > EXPIRE_TIME);
    }
}
