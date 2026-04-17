package com.hyltest.rag_practice.controller;

import com.hyltest.rag_practice.entity.vo.ChatHistoryVO;
import com.hyltest.rag_practice.entity.vo.MessageVO;
import com.hyltest.rag_practice.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    private final ChatMemory chatMemory;

    /**
     * 获取指定类型的所有会话历史列表
     * 返回包含 ID、标题和文件名的列表
     * 
     * @param type 会话类型（如 "pdf"）
     * @return 会话历史列表
     */
    @GetMapping("/{type}")
    public List<ChatHistoryVO> getChatHistoryList(@PathVariable("type") String type) {
        log.info("获取会话历史列表，类型: {}", type);
        return chatHistoryRepository.getChatIds(type);
    }

    /**
     * 获取指定会话的消息历史
     * 
     * @param type 会话类型
     * @param chatId 会话 ID
     * @return 消息列表
     */
    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatMessages(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        log.info("获取会话消息历史，chatId: {}", chatId);
        List<Message> messages = chatMemory.get(chatId);
        if(messages == null) {
            return List.of();
        }
        return messages.stream().map(MessageVO::new).toList();
    }
}
