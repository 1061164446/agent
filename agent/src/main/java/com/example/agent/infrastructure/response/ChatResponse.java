package com.example.agent.infrastructure.response;

/**
 * 聊天响应对象
 */
public class ChatResponse {

    /**
     * 用户消息
     */
    private String message;

    /**
     * AI回复
     */
    private String response;

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

    /**
     * 获取AI回复
     * @return AI回复
     */
    public String getResponse() {
        return response;
    }

    /**
     * 设置AI回复
     * @param response AI回复
     */
    public void setResponse(String response) {
        this.response = response;
    }
} 