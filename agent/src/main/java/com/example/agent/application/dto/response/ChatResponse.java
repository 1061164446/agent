package com.example.agent.application.dto.response;

import lombok.Data;

/**
 * 聊天响应对象
 * 
 * 用于返回AI助手的回复消息，包含以下字段：
 * - response: AI助手的回复内容
 * 
 * 响应规则：
 * - 响应内容不能为空
 * - 响应内容长度不能超过2000字符
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
@Data
public class ChatResponse {

    /**
     * AI助手的回复内容
     * 
     * 响应规则：
     * - 不能为空
     * - 长度不能超过2000字符
     * 
     * @example "你好！我是AI助手，很高兴为你服务。请问有什么可以帮助你的？"
     */
    private String response;
} 