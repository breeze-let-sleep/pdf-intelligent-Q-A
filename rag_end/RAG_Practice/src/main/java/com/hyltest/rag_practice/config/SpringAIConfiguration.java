package com.hyltest.rag_practice.config;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * 配置向量存储、RAG 聊天客户端等核心组件
 */
@Configuration
public class SpringAIConfiguration {

    /**
     * 配置向量存储
     * 使用 SimpleVectorStore 作为内存向量数据库
     * 适合本地开发测试环境，生产环境建议使用 Milvus、ES 等专业向量数据库
     */
    @Bean
    public VectorStore vectorStore(DashScopeEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 配置 PDF 专用聊天客户端
     * 包含向量检索（QuestionAnswerAdvisor）和对话记忆（MessageChatMemoryAdvisor）
     */
    @Bean
    public ChatClient pdfChatClient(
            DeepSeekChatModel model,
            ChatMemory chatMemory,
            VectorStore vectorStore) {
        return ChatClient.builder(model)
                .defaultAdvisors(
                        // 日志顾问，便于调试 RAG 检索内容
                        SimpleLoggerAdvisor.builder().build(),
                        // 对话记忆顾问，支持多会话
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // RAG 核心：向量检索顾问
                        QuestionAnswerAdvisor
                                .builder(vectorStore)
                                .searchRequest(
                                        SearchRequest.builder()
                                                .similarityThreshold(0.5d) // 相似度阈值
                                                .topK(2) // 返回的文档片段数量
                                                .build()
                                ).build()
                )
                .build();
    }
}
