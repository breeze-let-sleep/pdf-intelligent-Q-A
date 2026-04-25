package com.hyltest.rag_practice.controller;

import com.hyltest.rag_practice.entity.vo.Result;
import com.hyltest.rag_practice.service.IFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PDF 相关控制器
 * 负责文件上传、下载、对话等接口
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {

    /**
     * JdbcChatMemoryRepository 实例
     * 用于持久化对话历史到数据库
     */
    private final JdbcChatMemoryRepository chatMemoryRepository;

    /**
     * 向量存储实例
     * 用于向量检索
     */
    private final VectorStore vectorStore;

    /**
     * ChatMemory 实例
     * 用于管理对话记忆
     * 已由 pdfChatClient 的 defaultAdvisors 中的 MessageChatMemoryAdvisor 使用
     * 保留此注入供其他场景使用
     */
    private final ChatMemory chatMemory;

    /**
     * 文件服务
     */
    private final IFileService fileService;

    /**
     * PDF 聊天客户端
     * 包含 RAG 向量检索和对话记忆功能
     */
    @Qualifier("pdfChatClient")
    private final ChatClient pdfChatClient;

    /**
     * 通用聊天客户端
     * 默认包含向量检索（QuestionAnswerAdvisor）和对话记忆（MessageChatMemoryAdvisor）
     */
    @Qualifier("chatClient")
    private final ChatClient chatClient;

    /**
     * 文件上传接口
     * 将 PDF 文件上传到阿里云 OSS，并保存映射关系到数据库
     *
     * @param chatId 会话 ID
     * @param file  上传的 PDF 文件
     * @return 上传结果，包含文件 URL
     */
    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
        try {
            // 1. 校验文件是否为 PDF 格式
            if (!Objects.equals(file.getContentType(), "application/pdf")
                    && !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                return Result.fail("只能上传PDF文件！");
            }

            // 2. 保存文件到 OSS，并记录映射关系到数据库
            boolean success = fileService.save(chatId, file.getResource());
            if (!success) {
                return Result.fail("保存文件失败！");
            }

            // 3. 返回文件 URL 给前端
            String fileUrl = fileService.getFileUrl(chatId);
            return Result.ok(fileUrl);

        } catch (Exception e) {
            log.error("PDF 上传失败", e);
            return Result.fail("上传文件失败：" + e.getMessage());
        }
    }

    /**
     * 文件下载/预览接口
     * 从阿里云 OSS 下载文件并返回给前端
     * 通过 Content-Disposition: inline 头告诉浏览器在页面内预览而非下载
     *
     * @param chatId 会话 ID
     * @return PDF 文件流
     */
    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String chatId) throws IOException {
        // 1. 获取文件
        Resource resource = fileService.getFile(chatId);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // 2. 获取文件名并编码，写入响应头
        String filename = fileService.getFileName(chatId);
        if (filename == null || filename.isEmpty()) {
            filename = "document.pdf";
        }
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        // 3. 返回文件，设置 inline 让浏览器预览而非下载
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + encodedFilename + "\"")
                .body(resource);
    }

    /**
     * PDF 预览代理接口
     * 用于前端 iframe 预览 PDF，避免 OSS URL 直接访问触发下载的问题
     * 返回的文件带有正确的 Content-Type 和 Content-Disposition: inline 头
     *
     * @param chatId 会话 ID
     * @return PDF 文件流，支持浏览器内联预览
     */
    @GetMapping("/preview/{chatId}")
    public ResponseEntity<Resource> preview(@PathVariable("chatId") String chatId) throws IOException {
        // 复用 download 方法的逻辑，返回 inline 的 PDF 流
        return download(chatId);
    }

    /**
     * PDF 对话接口 - 基于 RAG 的问答
     * 使用流式响应，返回 AI 生成的回答
     * <p>
     * 注意：pdfChatClient 已在 SpringAIConfiguration 中配置了 defaultAdvisors
     * （SimpleLoggerAdvisor、MessageChatMemoryAdvisor、QuestionAnswerAdvisor）
     * 此处只需通过 advisors() 方法传递动态参数（conversationId、filterExpression），
     * 不要重复添加 advisor 实例，否则会引发 "No StreamAdvisors available to execute" 错误
     *
     * @param prompt 用户输入的问题
     * @param chatId 会话 ID
     * @return 流式文本响应
     */
    @GetMapping(value = "/chatRag", produces = "text/html;charset=utf-8")
    public Flux<String> generateRagFlux(String prompt, String chatId) {
        StringBuilder sb = new StringBuilder();

        // 1. 获取历史消息，添加用户新消息
        List<Message> msgList = chatMemoryRepository.findByConversationId(chatId);
        msgList.add(new UserMessage(prompt));

        // 2. 构建流式响应
        // 使用 pdfChatClient 的 defaultAdvisors，仅通过 advisors() 传递动态参数
        Flux<String> flux = pdfChatClient.prompt()
                .advisors(advisorSpec -> advisorSpec
                        // 传递会话 ID，MessageChatMemoryAdvisor 据此读取/保存对话历史
                        .param(ChatMemory.CONVERSATION_ID, chatId)
                        // 传递向量检索过滤条件，QuestionAnswerAdvisor 据此筛选当前会话的文档
                        .param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "chat_id == '" + chatId + "'")
                )
                .user(prompt)
                .stream()
                .content()
                .doOnNext(sb::append)  // 收集每个流式片段
                .doOnComplete(() -> {
                    // 流式结束后将 AI 回复存入记忆
                    msgList.add(new AssistantMessage(sb.toString()));
                    chatMemoryRepository.saveAll(chatId, msgList);
                })
                .doOnError(throwable -> {
                    // 出错时打印错误信息
                    System.err.println("AI 流式响应错误：" + throwable.getMessage());
                    throwable.printStackTrace();
                });

        return flux;
    }

    /**
     * PDF 对话接口 - 不基于 RAG 的问答，可以越过rag进行对话
     * 使用流式响应，返回 AI 生成的回答
     * <p>
     * 注意：pdfChatClient 已在 SpringAIConfiguration 中配置了 defaultAdvisors
     * （SimpleLoggerAdvisor、MessageChatMemoryAdvisor、QuestionAnswerAdvisor）
     * 此处只需通过 advisors() 方法传递动态参数（conversationId、filterExpression），
     * 不要重复添加 advisor 实例，否则会引发 "No StreamAdvisors available to execute" 错误
     *
     * @param prompt 用户输入的问题
     * @param chatId 会话 ID
     * @return 流式文本响应
     */
    @GetMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> generateChatFlux(String prompt, String chatId) {
        StringBuilder sb = new StringBuilder();
        List<Message> msgList = chatMemoryRepository.findByConversationId(chatId);
        msgList.add(new UserMessage(prompt));

        // ========== 第 1 步：手动向量检索，拿到文档 ==========
        SearchRequest searchRequest = SearchRequest.builder()
                .query(prompt)
                .topK(5)                                    // 取前 5 个最相关片段
                .similarityThreshold(0.7)                   // 相似度阈值
                .filterExpression("chat_id == '" + chatId + "'")  // 过滤当前会话的文档
                .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        // ========== 第 2 步：自己格式化文档内容 ==========
        String context = documents.stream()
                .map(doc -> {
                    // 可以从 metadata 里取来源、页码等信息
                    String source = doc.getMetadata().getOrDefault("source", "未知").toString();
                    return String.format("【来源：%s】\n%s", source, doc.getText());
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        // ========== 第 3 步：完全自定义 System Prompt，要求 LLM "理解并拓展" ==========
        // 这里是你最核心的控制点：提示词怎么写，LLM 就怎么回答
        String systemPrompt = """
        你是一位资深文档分析专家兼百科知识专家。请根据用户问题和提供的参考文档，按以下要求回答：
        
        1. 先理解文档中的核心事实和观点；
        2. 在文档基础上进行合理拓展、补充背景知识、多方面思考、给出更深入的见解；
        3. 如果文档信息不足，则以你自己的拓展理解回答，并明确告知用户"上传文档中未提及，以下内容为AI理解生成"；
        4. 如果有参考文档，回答时自然融入文档内容，避免说"根据参考文档..."；
        5. 回答时，请使用markdown语法回答用户问题。
        
        参考文档内容如下：
        =====================
        {context}
        =====================
        """;

        // ========== 第 4 步：调用 ChatClient，只保留 MessageChatMemoryAdvisor 做记忆 ==========
        return chatClient.prompt()
                .system(s -> s.text(systemPrompt).param("context", context))
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec
                        // 只保留对话记忆 Advisor，不再用 QuestionAnswerAdvisor
                        .param(ChatMemory.CONVERSATION_ID, chatId)
                )
                .stream()
                .content()
                .doOnNext(chunk -> sb.append(chunk))
                .doOnComplete(() -> {
                    // 流结束后保存 AI 回复到记忆
                    msgList.add(new AssistantMessage(sb.toString()));
                    chatMemoryRepository.saveAll(chatId, msgList);
                })
                .doOnError(throwable -> {
                    System.err.println("AI 流式响应错误：" + throwable.getMessage());
                    throwable.printStackTrace();
                });
    }

    /**
     * 获取会话对应的文件信息
     *
     * @param chatId 会话 ID
     * @return 文件信息（URL、文件名、标题）
     */
    @GetMapping("/info/{chatId}")
    public Result getFileInfo(@PathVariable("chatId") String chatId) {
        String url = fileService.getFileUrl(chatId);
        String fileName = fileService.getFileName(chatId);
        String title = fileService.getTitle(chatId);

        if (url == null || url.isEmpty()) {
            return Result.fail("未找到该会话的文件");
        }

        // 使用 HashMap 代替 Map.of()，避免 null value 导致 NullPointerException
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("fileName", fileName != null ? fileName : "document.pdf");
        data.put("title", title != null ? title : (fileName != null ? fileName : "PDF对话"));

        return Result.ok(data);
    }
}
