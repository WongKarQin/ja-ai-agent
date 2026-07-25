package com.jc.aiagent.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSignerUtil;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
public class JwtUtil {

    /**
     * JWT密钥（生产环境应放在配置中心）
     */
    private static final String SECRET = "AiAgent_Jwt_Secret_Key_2024!@#$";

    /**
     * Token有效期: 7天 (单位: 毫秒)
     */
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * 生成Token
     */
    public static String generateToken(Long userId, String email) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("email", email);
        payload.put(JWT.EXPIRES_AT, new Date(System.currentTimeMillis() + EXPIRE_TIME));
        payload.put(JWT.ISSUED_AT, new Date());
        return JWTUtil.createToken(payload, SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证Token是否有效
     */
    public static boolean verify(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            return JWTUtil.verify(token, SECRET.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserId(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Object userId = jwt.getPayload("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    /**
     * 从Token中获取邮箱
     */
    public static String getEmail(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Object email = jwt.getPayload("email");
        return email != null ? email.toString() : null;
    }
}
