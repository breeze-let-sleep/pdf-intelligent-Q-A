package com.hyltest.rag_practice;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * RAG 功能测试类
 * 测试向量存储和检索功能
 */
@SpringBootTest
public class PlusChatClient {

    @BeforeEach
    public void init(@Autowired VectorStore vectorStore) {
        // 添加测试文档
        Document document1 = Document.builder()
                .text("""
                        预订航班：
                        - 通过我们的网站或移动应用程序预订。
                        - 预订时需要全额付款。
                        - 确保个人信息（姓名、ID等）的准确性，因为更正可能会产生25美元的费用。
                        """)
                .build();

        Document document2 = Document.builder()
                .text("""
                        取消预订：
                        - 最晚在航班起飞前48小时取消。
                        - 取消费用：经济舱75美元，豪华经济舱50美元，商务舱25美元。
                        - 退款将在7个工作日内处理。
                        """)
                .build();

        // 存储向量（内部会自动向量化）
        vectorStore.add(List.of(document1, document2));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public VectorStore vectorStore(DashScopeEmbeddingModel embeddingModel) {
            return SimpleVectorStore.builder(embeddingModel).build();
        }
    }

    /**
     * RAG 测试：Spring AI 先调用向量库检索，再调用模型进行问答
     */
    @Test
    public void testRag(@Autowired VectorStore vectorStore,
                        @Autowired org.springframework.ai.deepseek.DeepSeekChatModel chatModel) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String content = chatClient.prompt()
                .system("你是一个智能助手")
                .user("退票多少钱")
                .advisors(
                        SimpleLoggerAdvisor.builder().build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .query("退票多少钱")
                                        .topK(5)
                                        .similarityThreshold(0.5)
                                        .build())
                                .build()
                ).call()
                .content();
        System.out.println("========== RAG 回答 ==========");
        System.out.println(content);
    }

    /**
     * 纯向量检索测试
     */
    @Test
    public void testVectorSearch(@Autowired VectorStore vectorStore) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query("退票")
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        System.out.println("========== 向量检索结果 ==========");
        for (Document document : documents) {
            System.out.println("文档内容: " + document.getText());
            System.out.println("相似度分数: " + document.getScore());
            System.out.println("---");
        }
    }
}
