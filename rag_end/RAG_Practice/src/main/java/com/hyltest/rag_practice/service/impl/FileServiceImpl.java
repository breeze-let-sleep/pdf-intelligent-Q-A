package com.hyltest.rag_practice.service.impl;

import com.hyltest.rag_practice.entity.mapper.FileMapperMapper;
import com.hyltest.rag_practice.entity.po.FileMapper;
import com.hyltest.rag_practice.repository.ChatHistoryRepository;
import com.hyltest.rag_practice.service.IFileService;
import com.hyltest.rag_practice.utils.AliyunOSSOperator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * 文件服务实现类
 * 负责 PDF 文件的 OSS 存储、向量化存储和检索
 * 使用纯 MyBatis Mapper 进行数据库操作，不依赖 MyBatis-Plus
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {

    /**
     * 阿里云对象存储操作实例
     */
    private final AliyunOSSOperator aliyunOSSOperator;

    /**
     * 向量存储实例
     */
    private final VectorStore vectorStore;

    /**
     * 聊天历史仓储 - 用于记录已上传的会话和文件映射关系
     */
    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * 文件映射 Mapper（用于查询操作）
     */
    private final FileMapperMapper fileMapperMapper;

    /**
     * 向量数据持久化文件名
     */
    private static final String VECTOR_STORE_FILE = "chat-pdf.json";

    @Override
    public boolean save(String chatId, Resource resource) {
        String filename = resource.getFilename();
        if (filename == null || filename.isEmpty()) {
            log.error("文件名为null或空");
            return false;
        }

        try {
            // 1. 上传文件到阿里云 OSS，获取文件 URL
            String fileUrl = aliyunOSSOperator.upload(
                    resource.getInputStream().readAllBytes(),
                    filename
            );
            log.info("文件已上传到 OSS: {}", fileUrl);

            // 2. 一次性保存文件映射关系到 MySQL 数据库（file_mapper 表）
            // chatHistoryRepository.save 内部会处理 file_mapper 表的插入/更新（含 URL）
            chatHistoryRepository.save("pdf", chatId, filename, fileUrl);

            // 3. 将 PDF 内容写入向量库（用于 RAG 检索）
            writeToVectorStore(resource, chatId);

            return true;
        } catch (Exception e) {
            log.error("保存文件失败", e);
            throw new RuntimeException("保存文件失败", e);
        }
    }

    @Override
    public Resource getFile(String chatId) {
        String fileUrl = getFileUrl(chatId);
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("No file URL found for chatId: {}", chatId);
            return null;
        }

        try {
            // 从 OSS 下载文件内容
            URL url = new URL(fileUrl);
            try (InputStream inputStream = url.openStream()) {
                byte[] bytes = inputStream.readAllBytes();
                String fileName = getFileName(chatId);
                return new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return fileName != null ? fileName : "document.pdf";
                    }
                };
            }
        } catch (Exception e) {
            log.error("从 OSS 下载文件失败: chatId={}", chatId, e);
            return null;
        }
    }

    @Override
    public String getFileUrl(String chatId) {
        FileMapper fileMapper = getFileMapper(chatId);
        return fileMapper != null ? fileMapper.getUrl() : null;
    }

    @Override
    public String getTitle(String chatId) {
        FileMapper fileMapper = getFileMapper(chatId);
        return fileMapper != null ? fileMapper.getTitle() : null;
    }

    @Override
    public String getFileName(String chatId) {
        FileMapper fileMapper = getFileMapper(chatId);
        return fileMapper != null ? fileMapper.getFileName() : null;
    }

    @Override
    public void updateFileInfo(String chatId, String title, String fileName, String url) {
        // 通过 chatId 查询已有记录
        FileMapper existing = fileMapperMapper.selectByChatId(chatId);

        if (existing != null) {
            existing.setTitle(title);
            existing.setFileName(fileName);
            existing.setUrl(url);
            fileMapperMapper.updateById(existing);
            log.info("更新文件映射信息: chatId={}", chatId);
        }
    }

    @Override
    public FileMapper getFileMapper(String chatId) {
        return fileMapperMapper.selectByChatId(chatId);
    }

    /**
     * 初始化：从向量存储文件恢复向量数据
     * PDF 文件与会话的映射关系从 file_mapper 数据库表获取
     */
    @PostConstruct
    private void init() {
        // 加载向量存储数据（持久化到本地文件不变）
        if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
            FileSystemResource vectorResource = new FileSystemResource(VECTOR_STORE_FILE);
            if (vectorResource.exists()) {
                try {
                    simpleVectorStore.load(vectorResource);
                    log.info("向量存储数据已从 {} 恢复", VECTOR_STORE_FILE);
                } catch (Exception e) {
                    log.warn("加载向量存储失败，将从空状态开始", e);
                }
            }
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

            // 5. 将向量数据持久化到文件（保持不变）
            persistVectorStore();

        } catch (Exception e) {
            log.error("Failed to write PDF to vector store", e);
            throw new RuntimeException("PDF 向量存储失败", e);
        }
    }

    /**
     * 将向量数据持久化到本地文件
     */
    private void persistVectorStore() {
        if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
            try {
                simpleVectorStore.save(new File(VECTOR_STORE_FILE));
                log.info("向量数据已持久化到 {}", VECTOR_STORE_FILE);
            } catch (Exception e) {
                log.error("持久化向量数据失败", e);
            }
        }
    }
}
