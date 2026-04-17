package com.hyltest.rag_practice.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天历史 VO
 * 用于前端展示聊天历史列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryVO {
    /**
     * 会话 ID
     */
    private String id;
    
    /**
     * 会话标题（通常是文件名）
     */
    private String title;
    
    /**
     * 文件名（可选）
     */
    private String fileName;
}
