package com.jc.aiagent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 状态: 1-正常 0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
