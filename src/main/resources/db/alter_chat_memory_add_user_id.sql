-- 为 chat_memory 表添加 user_id 字段并建立外键关联
-- 请在 MySQL 中执行以下 SQL

USE jc_ai_agent;

-- 添加 user_id 字段
ALTER TABLE chat_memory
    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID' AFTER chat_id,
    ADD INDEX idx_user_id (user_id);

-- 如果已有数据需要迁移，可执行以下更新（将现有数据关联到指定用户）
-- UPDATE chat_memory SET user_id = 1 WHERE user_id = 0;

-- 添加外键约束（可选，如果不需要强关联可跳过）
-- ALTER TABLE chat_memory
--     ADD CONSTRAINT fk_chat_memory_user
--     FOREIGN KEY (user_id) REFERENCES user(id)
--     ON DELETE CASCADE;
