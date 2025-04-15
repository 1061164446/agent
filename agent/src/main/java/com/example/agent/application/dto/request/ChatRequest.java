package com.example.agent.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求对象
 * 
 * 用于接收用户发送的聊天消息，包含以下字段：
 * - message: 用户发送的消息内容
 * 
 * 验证规则：
 * - 消息不能为空
 * - 消息长度不能超过2000字符
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
@Data
public class ChatRequest {

    /**
     * 用户发送的消息内容
     * 
     * 验证规则：
     * - 不能为空
     * - 长度不能超过2000字符
     * 
     * @example "你好，请问有什么可以帮助你的？"
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字符")
    private String message;
} 