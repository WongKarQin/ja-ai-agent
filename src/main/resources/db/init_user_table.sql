-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- CREATE DATABASE jc_ai_agent
-- USE jc_ai_agent;
--
-- CREATE TABLE `user` (
--                         `id` bigint NOT NULL AUTO_INCREMENT,
--                         `email` varchar(100) NOT NULL,
--                         `password` varchar(255) NOT NULL,
--                         `nickname` varchar(50) DEFAULT NULL,
--                         `status` tinyint DEFAULT '1',
--                         `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
--                         `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--                         PRIMARY KEY (`id`),
--                         UNIQUE KEY `email` (`email`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
--
-- CREATE TABLE `chat_memory` (
--                                `id` bigint NOT NULL AUTO_INCREMENT,
--                                `user_id` bigint NOT NULL,
--                                `content` text NOT NULL,
--                                `type` varchar(10) NOT NULL,
--                                `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
--                                `chat_id` varchar(36) NOT NULL,
--                                PRIMARY KEY (`id`),
--                                KEY `idx_user_id` (`user_id`),
--                                KEY `idx_timestamp` (`timestamp`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;