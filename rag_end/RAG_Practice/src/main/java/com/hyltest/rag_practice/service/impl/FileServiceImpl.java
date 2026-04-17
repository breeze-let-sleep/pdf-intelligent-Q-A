package com.hyltest.rag_practice.service.impl;

import com.hyltest.rag_practice.repository.ChatHistoryRepository;
import com.hyltest.rag_practice.service.IFileService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * 文件服务实现类
 * 负责 PDF 文件的存储、向量化存储和检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    /**
     * 向量存储实例
     */
    private final VectorStore vectorStore;

    /**
     * 聊天历史仓储 - 用于记录已上传的会话
     */
    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 会话ID与文件名的映射关系
     * 用于查询会话历史时重新加载文件
     */
    private final Properties chatFiles = new Properties();

    @Override
    public boolean save(String chatId, Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || filename.isEmpty()) {
            log.error("文件名为null或空");
            return false;
        }

        // 1. 保存到本地磁盘
        File target = new File(filename);
        if (!target.exists()) {
            try {
                Files.copy(resource.getInputStream(), target.toPath());
                log.info("PDF 保存: {}", filename);
            } catch (IOException e) {
                log.error("PDF 资源保存失败.", e);
                return false;
            }
        }

        // 2. 保存映射关系
        chatFiles.put(chatId, filename);

        // 3. 将 chatId 和文件名保存到聊天历史记录
        chatHistoryRepository.save("pdf", chatId, filename);
        log.info("Chat history recorded for chatId: {}, filename: {}", chatId, filename);

        // 4. 写入向量库
        writeToVectorStore(resource, chatId);
        return true;
    }

    @Override
    public Resource getFile(String chatId) {
        String filename = chatFiles.getProperty(chatId);
        if (filename == null) {
            log.warn("No file found for chatId: {}", chatId);
            return null;
        }
        Resource resource = new FileSystemResource(filename);
        if (!resource.exists()) {
            log.warn("File not found: {}", filename);
            return null;
        }
        return resource;
    }

    /**
     * 初始化：从磁盘恢复持久化的数据
     * 由于选择了基于内存的 SimpleVectorStore，重启会丢失向量数据
     * 所以这里将 PDF 文件与 chatId 的对应关系、VectorStore 都持久化到磁盘
     */
    @PostConstruct
    private void init() {
        // 1. 加载会话-文件映射关系
        FileSystemResource pdfResource = new FileSystemResource("chat-pdf.properties");
        if (pdfResource.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pdfResource.getInputStream(), StandardCharsets.UTF_8))) {
                chatFiles.load(reader);
                log.info("Loaded {} chat-file mappings", chatFiles.size());
            } catch (IOException e) {
                log.warn("Failed to load chat-pdf.properties", e);
            }
        }

        // 2. 加载向量存储数据
        FileSystemResource vectorResource = new FileSystemResource("chat-pdf.json");
        if (vectorResource.exists()) {
            try {
                SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
                simpleVectorStore.load(vectorResource);
                log.info("Vector store loaded from chat-pdf.json");
            } catch (Exception e) {
                log.warn("Failed to load vector store", e);
            }
        }

        // 3. 从 chat-pdf.properties 中恢复 chatHistory
        // 确保历史记录与文件映射保持一致
        if (!chatFiles.isEmpty()) {
            chatFiles.forEach((key, value) -> {
                String chatId = String.valueOf(key);
                String fileName = String.valueOf(value);
                chatHistoryRepository.save("pdf", chatId, fileName);
                log.debug("Restored chat history: {} -> {}", chatId, fileName);
            });
            log.info("Chat history restored from chat-pdf.properties, count: {}", chatFiles.size());
        }
    }

    /**
     * 销毁前持久化数据到磁盘
     */
    @PreDestroy
    private void persistent() {
        try {
            // 1. 保存会话-文件映射关系
            chatFiles.store(new FileWriter("chat-pdf.properties"), "RAG Practice Chat Files Mapping - " + LocalDateTime.now());

            // 2. 保存向量存储数据
            if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
                simpleVectorStore.save(new File("chat-pdf.json"));
                log.info("Vector store persisted to chat-pdf.json");
            }
            log.info("All data persisted successfully");
        } catch (IOException e) {
            log.error("Failed to persist data", e);
            throw new RuntimeException("数据持久化失败", e);
        }
    }

    /**
     * 将 PDF 文档内容写入向量库
     *
     * @param resource PDF 文件资源
     * @param chatId   会话 ID，用于过滤检索结果
     */
    private void writeToVectorStore(Resource resource, String chatId) {
        try {
            // 1. 创建 PDF 读取器
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                            .withPagesPerDocument(1) // 每1页PDF作为一个Document
                            .build()
            );

            // 2. 读取 PDF 文档，拆分为 Document
            List<Document> documents = reader.read();
            log.info("Read {} pages from PDF for chatId: {}", documents.size(), chatId);

            // 3. 为每个文档添加元数据（chat_id 用于后续过滤检索）
            String filename = resource.getFilename();
            documents.forEach(document -> {
                document.getMetadata().put("chat_id", chatId);
                document.getMetadata().put("file_name", filename != null ? filename : "unknown.pdf");
            });

            // 4. 写入向量库
            vectorStore.add(documents);
            log.info("Added {} documents to vector store for chatId: {}", documents.size(), chatId);

        } catch (Exception e) {
            log.error("Failed to write PDF to vector store", e);
            throw new RuntimeException("PDF 向量存储失败", e);
        }
    }
}
