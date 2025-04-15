package com.example.agent.domain.chat;

import com.example.agent.domain.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 聊天业务验证器
 * 负责验证聊天请求和响应的合法性
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
@Component
public class ChatValidator {

    /**
     * 验证聊天消息
     * @param message 消息内容
     * @throws BusinessException 当消息不符合业务规则时抛出
     */
    public void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (message.length() > 2000) {
            throw new BusinessException("消息长度不能超过2000字符");
        }
    }

    /**
     * 验证聊天请求
     * @param chatAggregate 聊天领域聚合对象
     * @throws BusinessException 当聊天请求不符合业务规则时抛出
     */
    public void validateChatRequest(ChatAggregate chatAggregate) {
        if (chatAggregate == null) {
            throw new BusinessException("聊天请求不能为空");
        }
        validateMessage(chatAggregate.getMessage());

        // 根据功能类型进行不同的验证
        if ("image".equals(chatAggregate.getFunctionType())) {
            validateImageParams(chatAggregate.getImageParams());
        } else if ("function".equals(chatAggregate.getFunctionType())) {
            validateFunctionParams(chatAggregate.getFunctionParams());
        }
    }

    /**
     * 验证图片生成参数
     * @param imageParams 图片生成参数
     * @throws BusinessException 当参数不合法时抛出
     */
    public void validateImageParams(ChatAggregate.ImageParams imageParams) {
        if (imageParams == null) {
            throw new BusinessException("图片生成参数不能为空");
        }

        // 验证图片大小
        if (imageParams.getSize() != null) {
            if (!"256x256".equals(imageParams.getSize()) && 
                !"512x512".equals(imageParams.getSize()) && 
                !"1024x1024".equals(imageParams.getSize())) {
                throw new BusinessException("图片大小必须是 256x256、512x512 或 1024x1024");
            }
        }

        // 验证图片数量
        if (imageParams.getN() != null) {
            if (imageParams.getN() < 1 || imageParams.getN() > 4) {
                throw new BusinessException("图片数量必须在 1-4 之间");
            }
        }

        // 验证图片质量
        if (imageParams.getQuality() != null) {
            if (!"standard".equals(imageParams.getQuality()) && 
                !"hd".equals(imageParams.getQuality())) {
                throw new BusinessException("图片质量必须是 standard 或 hd");
            }
        }
    }

    /**
     * 验证函数调用参数
     * @param functionParams 函数调用参数
     * @throws BusinessException 当参数不合法时抛出
     */
    public void validateFunctionParams(ChatAggregate.FunctionParams functionParams) {
        if (functionParams == null) {
            throw new BusinessException("函数调用参数不能为空");
        }

        // 验证函数名称
        if (functionParams.getName() == null || functionParams.getName().trim().isEmpty()) {
            throw new BusinessException("函数名称不能为空");
        }

        // 验证函数描述
        if (functionParams.getDescription() == null || functionParams.getDescription().trim().isEmpty()) {
            throw new BusinessException("函数描述不能为空");
        }

        // 验证函数参数
        if (functionParams.getArguments() == null || functionParams.getArguments().trim().isEmpty()) {
            throw new BusinessException("函数参数不能为空");
        }
    }

    /**
     * 验证聊天响应
     * @param chatAggregate 聊天领域聚合对象
     * @throws BusinessException 当聊天响应不符合业务规则时抛出
     */
    public void validateChatResponse(ChatAggregate chatAggregate) {
        if (chatAggregate == null) {
            throw new BusinessException("聊天响应不能为空");
        }
        validateMessage(chatAggregate.getResponse());
    }
} 