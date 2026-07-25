package com.jc.aiagent.controller;

import com.jc.aiagent.common.Result;
import com.jc.aiagent.dto.UserLoginDTO;
import com.jc.aiagent.dto.UserRegisterDTO;
import com.jc.aiagent.dto.VerifyCodeDTO;
import com.jc.aiagent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-verify-code")
    public Result<Void> sendVerifyCode(@Valid @RequestBody VerifyCodeDTO dto) {
        try {
            userService.sendVerifyCode(dto);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("发送验证码失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        try {
            userService.register(dto);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("用户注册失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO dto) {
        try {
            String token = userService.login(dto);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return Result.success(data);
        } catch (RuntimeException e) {
            log.warn("用户登录失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查邮箱是否已注册
     */
    @GetMapping("/check-email")
    public Result<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean registered = userService.isEmailRegistered(email);
        Map<String, Object> data = new HashMap<>();
        data.put("registered", registered);
        return Result.success(data);
    }
}
