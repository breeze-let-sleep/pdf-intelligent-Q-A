-- Spring AI Chat Memory MySQL Schema
-- 用于 JdbcChatMemoryRepository 自动建表（initialize-schema: always）
-- Spring AI 官方 jar 中未提供 MySQL 版本，需手动补充此文件
-- 注意：MySQL 不支持 CREATE INDEX IF NOT EXISTS（MariaDB 特有），需要改用存储过程或直接创建索引

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL,
    `timestamp` TIMESTAMP NOT NULL,
    CONSTRAINT TYPE_CHECK CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    INDEX SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX (conversation_id, `timestamp`)
);
