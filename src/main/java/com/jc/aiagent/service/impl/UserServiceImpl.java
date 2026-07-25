package com.jc.aiagent.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.jc.aiagent.dto.UserLoginDTO;
import com.jc.aiagent.dto.UserRegisterDTO;
import com.jc.aiagent.dto.VerifyCodeDTO;
import com.jc.aiagent.entity.User;
import com.jc.aiagent.mapper.UserMapper;
import com.jc.aiagent.service.UserService;
import com.jc.aiagent.utils.JwtUtil;
import com.jc.aiagent.utils.VerifyCodeCache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    /**
     * 密码正则: 8-20位, 必须包含数字、小写字母、大写字母
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,20}$");

    @Override
    public void sendVerifyCode(VerifyCodeDTO dto) {
        String email = dto.getEmail();

        // 检查发送间隔
        if (!VerifyCodeCache.canSend(email)) {
            throw new RuntimeException("发送过于频繁，请60秒后再试");
        }

        // 检查邮箱是否已注册
        if (userMapper.countByEmail(email) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);
        VerifyCodeCache.put(email, code);

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("【AI智能应用中心】注册验证码");
            message.setText("您的注册验证码是: " + code + "\n验证码有效期为5分钟，请勿泄露给他人。");
            mailSender.send(message);
            log.info("验证码已发送至邮箱: {}", email);
        } catch (Exception e) {
            log.error("发送验证码邮件失败", e);
            throw new RuntimeException("发送验证码失败，请检查邮箱配置");
        }
    }

    @Override
    public void register(UserRegisterDTO dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();
        String confirmPassword = dto.getConfirmPassword();
        String verifyCode = dto.getVerifyCode();

        // 1. 校验邮箱格式
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("邮箱格式不正确");
        }

        // 2. 校验密码复杂度
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RuntimeException("密码长度需8-20位，且必须包含数字、小写字母、大写字母");
        }

        // 3. 校验两次密码是否一致
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 4. 校验验证码
        if (!VerifyCodeCache.verify(email, verifyCode)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 5. 检查邮箱是否已注册
        if (userMapper.countByEmail(email) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 6. 加密密码并保存用户
        String encryptedPassword = DigestUtil.bcrypt(password);
        User user = User.builder()
                .email(email)
                .password(encryptedPassword)
                .nickname(dto.getNickname())
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.insert(user);

        // 7. 注册成功后删除验证码
        VerifyCodeCache.remove(email);
    }

    @Override
    public String login(UserLoginDTO dto) {
        String email = dto.getEmail();
        String password = dto.getPassword();

        // 1. 查询用户
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 检查用户状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        // 3. 校验密码
        if (!DigestUtil.bcryptCheck(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 4. 生成JWT Token
        return JwtUtil.generateToken(user.getId(), user.getEmail());
    }

    @Override
    public boolean isEmailRegistered(String email) {
        return userMapper.countByEmail(email) > 0;
    }
}
