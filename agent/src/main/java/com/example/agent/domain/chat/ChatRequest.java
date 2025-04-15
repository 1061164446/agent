package com.example.agent.domain.chat;

import lombok.Data;

/**
 * 聊天请求对象
 */
@Data
public class ChatRequest {
    /**
     * 用户消息
     */
    private String message;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 上下文消息列表
     */
    private String[] contextMessages;
} 