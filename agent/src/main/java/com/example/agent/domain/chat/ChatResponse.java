package com.example.agent.domain.chat;

import lombok.Data;

/**
 * 聊天响应对象
 */
@Data
public class ChatResponse {
    /**
     * 响应消息
     */
    private String message;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 响应数据
     */
    private Object data;
} 