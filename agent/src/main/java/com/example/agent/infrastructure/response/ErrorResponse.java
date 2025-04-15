package com.example.agent.infrastructure.response;

/**
 * 错误响应对象
 */
public class ErrorResponse {

    /**
     * 错误信息
     */
    private String message;

    /**
     * 获取错误信息
     * @return 错误信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置错误信息
     * @param message 错误信息
     */
    public void setMessage(String message) {
        this.message = message;
    }
} 