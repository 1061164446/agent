package com.example.agent.application.service;

import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.infrastructure.impl.ChatServiceImpl;
import reactor.core.publisher.Flux;

/**
 * 聊天服务接口
 * 
 * 提供聊天相关的业务功能，包括：
 * - 处理用户消息并获取AI回复
 * - 支持流式对话
 * - 支持多轮对话
 * - 支持图片生成
 * - 支持函数调用
 * - 支持语音识别
 * 
 * 实现类：{@link ChatServiceImpl}
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
public interface ChatService {

    /**
     * 处理聊天消息
     * 
     * 接收用户消息，调用AI服务获取回复，并返回处理结果
     * 支持文本对话、图片生成、函数调用等功能
     * 
     * @param chatAggregate 聊天领域聚合对象，包含用户消息和功能类型
     * @return 更新后的聊天领域聚合对象，包含AI回复
     * 
     * @throws com.example.agent.application.exception.BusinessException 当业务逻辑验证失败时抛出
     * @throws org.springframework.web.client.ResourceAccessException 当AI服务连接超时时抛出
     * @throws RuntimeException 当其他异常发生时抛出
     */
    ChatAggregate processChat(ChatAggregate chatAggregate);

    /**
     * 处理流式聊天消息
     * 
     * 接收用户消息，调用AI服务获取流式回复
     * 支持实时返回AI的回复内容
     * 
     * @param chatAggregate 聊天领域聚合对象，包含用户消息
     * @return 流式响应，包含AI的实时回复
     * 
     * @throws com.example.agent.application.exception.BusinessException 当业务逻辑验证失败时抛出
     * @throws org.springframework.web.client.ResourceAccessException 当AI服务连接超时时抛出
     * @throws RuntimeException 当其他异常发生时抛出
     */
    Flux<String> processStreamMessage(ChatAggregate chatAggregate);

    /**
     * 处理图片生成请求
     * 
     * 根据用户描述生成图片
     * 
     * @param chatAggregate 聊天领域聚合对象，包含图片生成参数
     * @return 更新后的聊天领域聚合对象，包含生成的图片URL
     * 
     * @throws com.example.agent.application.exception.BusinessException 当业务逻辑验证失败时抛出
     * @throws org.springframework.web.client.ResourceAccessException 当AI服务连接超时时抛出
     * @throws RuntimeException 当其他异常发生时抛出
     */
    ChatAggregate processImageGeneration(ChatAggregate chatAggregate);
} 