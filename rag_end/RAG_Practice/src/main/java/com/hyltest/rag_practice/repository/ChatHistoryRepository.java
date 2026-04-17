package com.hyltest.rag_practice.repository;

import com.hyltest.rag_practice.entity.vo.ChatHistoryVO;

import java.util.List;

/**
 * 聊天历史仓储接口
 * 负责管理会话ID列表和对话消息历史的持久化
 */
public interface ChatHistoryRepository {
    
    /**
     * 保存会话 ID 和文件名的映射
     * @param type 会话类型（如 "pdf"）
     * @param chatId 会话 ID
     * @param fileName 文件名（可选）
     */
    void save(String type, String chatId, String fileName);
    
    /**
     * 保存会话 ID（无文件名）
     * @param type 会话类型
     * @param chatId 会话 ID
     */
    default void save(String type, String chatId) {
        save(type, chatId, null);
    }
    
    /**
     * 获取指定类型的所有会话历史
     * @param type 会话类型
     * @return 会话历史列表（包含 ID 和文件名）
     */
    List<ChatHistoryVO> getChatIds(String type);
}
