package com.example.agent.infrastructure.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求对象
 */
public class ChatRequest {

    /**
     * 用户消息
     */
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字符")
    private String message;

    /**
     * 获取用户消息
     * @return 用户消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置用户消息
     * @param message 用户消息
     */
    public void setMessage(String message) {
        this.message = message;
    }
} 