package com.example.agent.application.dto.response;

/**
 * 聊天响应数据传输对象
 * 用于返回聊天服务的响应结果
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
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

    /**
     * 获取响应消息
     * @return 响应消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置响应消息
     * @param message 响应消息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置是否成功
     * @param success 是否成功
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取错误信息
     * @return 错误信息
     */
    public String getError() {
        return error;
    }

    /**
     * 设置错误信息
     * @param error 错误信息
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * 获取响应数据
     * @return 响应数据
     */
    public Object getData() {
        return data;
    }

    /**
     * 设置响应数据
     * @param data 响应数据
     */
    public void setData(Object data) {
        this.data = data;
    }
} 