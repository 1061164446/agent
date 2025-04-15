package com.example.agent.domain.exception;

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {

    /**
     * 构造业务异常
     * @param message 异常信息
     */
    public BusinessException(String message) {
        super(message);
    }
} 