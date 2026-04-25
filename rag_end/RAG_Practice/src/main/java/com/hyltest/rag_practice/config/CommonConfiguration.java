package com.hyltest.rag_practice.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用配置类
 * 提供 ChatMemory 等通用 Bean
 */
@Configuration
public class CommonConfiguration {

    /**
     * 配置数据库聊天记忆，用于存储对话历史
     * 使用 Spring AI 官方 JdbcChatMemoryRepository 持久化到 MySQL
     * Spring AI 会自动创建 AI_CONVERSATION 等数据表
     *
     * @param chatMemoryRepository Spring AI 自动注入的 JDBC 聊天记忆仓库
     * @return ChatMemory 实例
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)  // 配置 JDBC 持久化
                .maxMessages(100)  // 最多保留100条消息
                .build();
    }
}
