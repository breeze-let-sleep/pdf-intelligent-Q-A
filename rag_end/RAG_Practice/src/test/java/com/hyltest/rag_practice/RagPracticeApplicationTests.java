package com.hyltest.rag_practice;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;

/**
 * RAG 完整功能测试类
 * 包含向量数据库测试、PDF 读取测试、Embedding 测试
 *
 * 向量数据库选择说明：
 * 1. SimpleVectorStore: 适用于简单的向量数据库场景，例如：本地向量数据库、学习使用。
 * 2. MilvusVectorStore: 比较流行的向量数据库，支持分布式、高可用、海量数据存储。
 * 3. ElasticsearchVectorStore: 如果项目中有 ES 可以考虑使用。
 */
@SpringBootTest
class RagPracticeApplicationTests {

    /**
     * 测试向量数据库存储和检索功能
     */
    @Test
    public void testVectorStore(@Autowired VectorStore vectorStore) {
        Document document1 = Document.builder()
                .text("""
                        预订航班：
                        - 通过我们的网站或移动应用程序预订。
                        - 预订时需要全额付款。
                        - 确保个人信息（姓名、ID等）的准确性，因为更正可能会产生25美元的费用。
                        """)
                .metadata("category", "booking")
                .build();

        Document document2 = Document.builder()
                .text("""
                        取消预订：
                        - 最晚在航班起飞前48小时取消。
                        - 取消费用：经济舱75美元，豪华经济舱50美元，商务舱25美元。
                        - 退款将在7个工作日内处理。
                        """)
                .metadata("category", "cancellation")
                .build();

        // 存储向量（内部会自动向量化）
        vectorStore.add(List.of(document1, document2));

        // 简单查询
        List<Document> simpleSearch = vectorStore.similaritySearch("退票");
        System.out.println("========== 简单查询结果 ==========");
        for (Document search : simpleSearch) {
            System.out.println(search.getText());
            System.out.println("分数: " + search.getScore());
        }

        // 高级查询（带参数）
        SearchRequest searchRequest = SearchRequest.builder()
                .query("退票")
                .topK(5)
                .similarityThreshold(0.5)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        System.out.println("\n========== 高级查询结果 ==========");
        for (Document document : documents) {
            System.out.println(document.getText());
            System.out.println("分数: " + document.getScore());
        }
    }

    /**
     * 测试 PDF 文档读取和向量化存储
     */
    @Test
    public void testStore() {
        // 使用项目中的 Linux 命令 PDF
        Resource resource = new FileSystemResource("常用Linux命令及快捷键.pdf");

        // 1. 创建 PDF 读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );

        // 2. 读取 PDF 并拆分为文档
        List<Document> documents = reader.read();
        System.out.println("========== PDF 读取结果 ==========");
        System.out.println("读取到 " + documents.size() + " 页文档");

        // 注意：这里需要通过 @Autowired 获取 vectorStore，但测试方法不能直接注入
        // 实际使用中会通过 ApplicationContext 获取或使用 @SpringBootTest 注入
    }

    /**
     * 测试 Embedding 模型
     * 向量化文本，查看向量维度
     */
    @Test
    public void testEmbedding(@Autowired DashScopeEmbeddingModel embeddingModel) {
        float[] embedding = embeddingModel.embed("我是李四");
        System.out.println("========== Embedding 测试 ==========");
        System.out.println("向量维度: " + embedding.length);
        System.out.println("向量前5个值: " + Arrays.toString(Arrays.copyOf(embedding, Math.min(5, embedding.length))));
    }

    /**
     * 测试向量检索（带元数据过滤）
     */
    @Test
    public void testFilteredSearch(@Autowired VectorStore vectorStore) {
        // 先添加一些带元数据的文档
        Document doc1 = Document.builder()
                .text("这是关于 Java 编程的文档")
                .metadata("file_name", "java-guide.pdf")
                .metadata("category", "programming")
                .build();

        Document doc2 = Document.builder()
                .text("这是关于 Python 编程的文档")
                .metadata("file_name", "python-guide.pdf")
                .metadata("category", "programming")
                .build();

        Document doc3 = Document.builder()
                .text("这是一份财务报表")
                .metadata("file_name", "report.pdf")
                .metadata("category", "finance")
                .build();

        vectorStore.add(List.of(doc1, doc2, doc3));

        // 带过滤条件的检索
        SearchRequest request = SearchRequest.builder()
                .query("编程语言")
                .topK(5)
                .similarityThreshold(0.5)
                // 过滤条件：只检索编程类文档
                .filterExpression("category == 'programming'")
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);
        System.out.println("========== 过滤检索结果 ==========");
        System.out.println("找到 " + docs.size() + " 条相关文档");
        for (Document doc : docs) {
            System.out.println("内容: " + doc.getText());
            System.out.println("文件: " + doc.getMetadata().get("file_name"));
            System.out.println("---");
        }
    }
}
