package com.hyltest.rag_practice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.hyltest.rag_practice.entity.po.Msg;
import com.hyltest.rag_practice.entity.vo.ChatHistoryVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 聊天历史仓储的内存实现
 * 负责管理会话ID列表和对话消息历史的持久化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    private static final String HISTORY_FILE = "chat-history.json";
    private static final String MEMORY_FILE = "chat-memory.json";

    /**
     * 会话历史记录：type -> 包含 id 和 fileName 的列表
     */
    private Map<String, List<ChatHistoryVO>> chatHistory;

    /**
     * chatId 到文件名的映射（用于快速查找）
     */
    private Map<String, String> chatIdToFileName;

    private final ObjectMapper objectMapper;

    private final ChatMemory chatMemory;

    @Override
    public void save(String type, String chatId, String fileName) {
        List<ChatHistoryVO> chatIds = chatHistory.computeIfAbsent(type, k -> new ArrayList<>());
        
        boolean exists = chatIds.stream().anyMatch(vo -> vo.getId().equals(chatId));
        if (exists) {
            chatIds.stream()
                    .filter(vo -> vo.getId().equals(chatId))
                    .findFirst()
                    .ifPresent(vo -> {
                        vo.setFileName(fileName);
                        vo.setTitle(fileName != null ? fileName : generateDefaultTitle(chatId));
                    });
            return;
        }
        
        String title = fileName != null ? fileName : generateDefaultTitle(chatId);
        chatIds.add(new ChatHistoryVO(chatId, title, fileName));
        
        if (fileName != null) {
            chatIdToFileName.put(chatId, fileName);
        }
    }

    @Override
    public List<ChatHistoryVO> getChatIds(String type) {
        List<ChatHistoryVO> result = chatHistory.getOrDefault(type, List.of());
        return result.stream()
                .map(vo -> {
                    if (vo.getTitle() == null || vo.getTitle().isEmpty()) {
                        String fileName = vo.getFileName() != null ? vo.getFileName() : chatIdToFileName.get(vo.getId());
                        vo.setTitle(fileName != null ? fileName : generateDefaultTitle(vo.getId()));
                    }
                    return vo;
                })
                .toList();
    }

    public String getFileName(String chatId) {
        return chatIdToFileName.get(chatId);
    }

    private String generateDefaultTitle(String chatId) {
        return "PDF对话 " + chatId.substring(Math.max(0, chatId.length() - 6));
    }

    @PostConstruct
    private void init() {
        log.info("初始化 ChatHistoryRepository...");
        
        this.chatHistory = new HashMap<>();
        this.chatIdToFileName = new HashMap<>();
        
        loadHistory();
        loadMemory();
        
        registerShutdownHook();
    }

    private void loadHistory() {
        FileSystemResource resource = new FileSystemResource(HISTORY_FILE);
        if (!resource.exists()) {
            log.info("History 文件 {} 找不到, 重新开始", HISTORY_FILE);
            return;
        }
        
        try (InputStream is = resource.getInputStream()) {
            Map<String, List<ChatHistoryVO>> loaded = objectMapper.readValue(is, new TypeReference<>() {});
            if (loaded != null) {
                this.chatHistory = loaded;
                loaded.values().stream()
                        .flatMap(List::stream)
                        .filter(vo -> vo.getFileName() != null)
                        .forEach(vo -> chatIdToFileName.put(vo.getId(), vo.getFileName()));
                log.info("载入大小为： {} 的 chat history", chatHistory.size());
            }
        } catch (IOException e) {
            log.warn("载入失败 {}, history 最开始为空", HISTORY_FILE, e);
        }
    }

    private void loadMemory() {
        FileSystemResource resource = new FileSystemResource(MEMORY_FILE);
        if (!resource.exists()) {
            log.info("Memory 文件 {} 找不到, 重新开始", MEMORY_FILE);
            return;
        }
        
        try (InputStream is = resource.getInputStream()) {
            Map<String, List<Msg>> memory = objectMapper.readValue(is, new TypeReference<>() {});
            if (memory != null) {
                memory.forEach((chatId, msgs) -> 
                    chatMemory.add(chatId, msgs.stream().map(Msg::toMessage).toList())
                );
                log.info("载入大小为： {} 的 chat memories", memory.size());
            }
        } catch (IOException e) {
            log.warn("载入失败 {}, memory 最开始为空", MEMORY_FILE, e);
        }
    }

    @PreDestroy
    private void preDestroy() {
        log.info("PreDestroy: persisting chat data...");
        persistAll();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("ShutdownHook triggered: persisting chat data...");
            persistAll();
        }, "ChatHistory-ShutdownHook"));
    }

    private synchronized void persistAll() {
        persistHistory();
        persistMemory();
    }

    /**
     * 根据文件类型持久化所有会话（session）
     * 一次“新对话”表示一个会话
     */
    private void persistHistory() {
        String json = toJsonString(this.chatHistory);
        if (json == null) {
            log.error("chat history 序列化失败");
            return;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(HISTORY_FILE), true, StandardCharsets.UTF_8)) {
            writer.write(json);
            log.info("Chat history 持久化到 {} ({} types)", HISTORY_FILE, chatHistory.size());
        } catch (IOException e) {
            log.error("chat history 持久化失败", e);
        }
    }

    /**
     * 持久化所有对话记忆（memory）
     */
    private void persistMemory() {
        Map<String, List<Msg>> memoryToSave = extractMemoryFromChatMemory();
        if (memoryToSave.isEmpty()) {
            log.info("没有chat memory可以持久化");
            return;
        }
        
        String json = toJsonString(memoryToSave);
        if (json == null) {
            log.error("chat memory 序列化失败");
            return;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(MEMORY_FILE), true, StandardCharsets.UTF_8)) {
            writer.write(json);
            log.info("Chat memory 持久化到 {} ({} conversations)", MEMORY_FILE, memoryToSave.size());
        } catch (IOException e) {
            log.error("持久化失败： chat memory", e);
        }
    }

    /**
     * 从 ChatMemory 中提取消息记录
     * 使用公共 API，无需反射
     */
    private Map<String, List<Msg>> extractMemoryFromChatMemory() {
        Map<String, List<Msg>> result = new HashMap<>();
        
        // 获取内部的 InMemoryChatMemoryRepository
        if (!(chatMemory instanceof org.springframework.ai.chat.memory.MessageWindowChatMemory)) {
            log.warn("ChatMemory 不是 MessageWindowChatMemory，不能提取内存");
            return result;
        }
        
        try {
            // 通过反射获取 chatMemoryRepository 字段
            var repoField = chatMemory.getClass().getDeclaredField("chatMemoryRepository");
            repoField.setAccessible(true);
            Object repo = repoField.get(chatMemory);
            
            if (repo instanceof InMemoryChatMemoryRepository inMemoryRepo) {
                // 使用公共 API 获取所有会话 ID
                List<String> conversationIds = inMemoryRepo.findConversationIds();
                
                for (String chatId : conversationIds) {
                    List<Message> messages = inMemoryRepo.findByConversationId(chatId);
                    if (messages != null && !messages.isEmpty()) {
                        result.put(chatId, messages.stream().map(Msg::new).toList());
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("未能访问chatMemoryRepository字段", e);
        }
        
        return result;
    }

    private String toJsonString(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }
}
