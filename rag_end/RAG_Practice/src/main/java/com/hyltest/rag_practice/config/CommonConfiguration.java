package com.hyltest.rag_practice.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用配置类
 * 提供 ChatMemory 等通用 Bean
 */
@Configuration
public class CommonConfiguration {

    /**
     * 配置内存聊天记忆，用于存储对话历史
     * 支持多会话，每个会话通过 conversationId 隔离
     * 使用 MessageWindowChatMemory 限制保留的对话消息数量
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100) // 最多保留100条消息
                .build();
    }
}
