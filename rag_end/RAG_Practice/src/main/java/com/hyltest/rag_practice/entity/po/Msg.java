package com.hyltest.rag_practice.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.*;

import java.util.Map;

/**
 * 消息实体类
 * 用于序列化/反序列化对话消息，支持持久化存储
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Msg {
    /**
     * 消息类型：USER、ASSISTANT、SYSTEM
     */
    MessageType messageType;
    
    /**
     * 消息文本内容
     */
    String text;
    
    /**
     * 消息元数据
     */
    Map<String, Object> metadata;

    /**
     * 从 Message 对象构造 Msg
     *
     * @param message Spring AI Message 对象
     */
    public Msg(Message message) {
        this.messageType = message.getMessageType();
        this.text = message.getText();
        this.metadata = message.getMetadata();
    }

    /**
     * 将 Msg 转换为 Spring AI Message 对象
     * 根据消息类型创建对应的 Message 实现类
     *
     * @return Spring AI Message 对象
     */
    public Message toMessage() {
        return switch (messageType) {
            case SYSTEM -> new SystemMessage(text);
            case USER -> UserMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .build();
            case ASSISTANT -> new AssistantMessage(text, metadata);
            default -> throw new IllegalArgumentException("Unsupported message type: " + messageType);
        };
    }
}
