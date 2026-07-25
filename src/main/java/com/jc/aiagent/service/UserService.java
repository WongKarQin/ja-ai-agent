package com.jc.aiagent.service;

import com.jc.aiagent.dto.UserLoginDTO;
import com.jc.aiagent.dto.UserRegisterDTO;
import com.jc.aiagent.dto.VerifyCodeDTO;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 发送邮箱验证码
     */
    void sendVerifyCode(VerifyCodeDTO dto);

    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录
     * @return Token
     */
    String login(UserLoginDTO dto);

    /**
     * 检查邮箱是否已注册
     */
    boolean isEmailRegistered(String email);
}
