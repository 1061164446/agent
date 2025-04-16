package com.example.agent.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 聊天请求数据传输对象
 * 用于接收前端发送的聊天请求
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
public class ChatRequest {
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 功能类型
     * text: 文本对话
     * image: 图片生成
     * speech: 语音识别
     * function: 函数调用
     */
    @NotBlank(message = "功能类型不能为空")
    private String functionType;

    /**
     * 获取消息内容
     * @return 消息内容
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置消息内容
     * @param content 消息内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取会话ID
     * @return 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话ID
     * @param sessionId 会话ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取功能类型
     * @return 功能类型
     */
    public String getFunctionType() {
        return functionType;
    }

    /**
     * 设置功能类型
     * @param functionType 功能类型
     */
    public void setFunctionType(String functionType) {
        this.functionType = functionType;
    }
} 