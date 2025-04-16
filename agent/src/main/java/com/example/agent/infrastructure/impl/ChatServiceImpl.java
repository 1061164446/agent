package com.example.agent.infrastructure.impl;

import com.alibaba.cloud.ai.tongyi.chat.TongYiChatModel;
import com.alibaba.cloud.ai.tongyi.chat.TongYiChatOptions;

import com.alibaba.cloud.ai.tongyi.image.TongYiImagesModel;
import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.chat.ChatValidator;
import com.example.agent.application.exception.BusinessException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务实现类
 * 提供文本对话、图片生成、语音识别、函数调用等功能
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final TongYiChatModel tongYiChatModel;
    private final ChatValidator chatValidator;

    /**
     * 构造函数
     * @param tongYiChatModel 通义千问模型
     * @param tongYiImageModel 通义千问图片模型
     * @param chatValidator 聊天验证器
     */
    public ChatServiceImpl(TongYiChatModel tongYiChatModel, TongYiImagesModel tongYiImageModel, ChatValidator chatValidator) {
        this.tongYiChatModel = tongYiChatModel;
        this.chatValidator = chatValidator;
    }

    /**
     * 处理流式聊天请求
     * @param chatAggregate 聊天聚合对象
     * @return 流式响应
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @Override
    public Flux<String> processStreamMessage(ChatAggregate chatAggregate) {
        try {
            // 验证聊天请求
            chatValidator.validateChatRequest(chatAggregate);

            // 调用通义千问API的流式接口
            TongYiChatOptions options = TongYiChatOptions.builder()
                    .withModel("qwen-turbo")
                    .withTemperature(0.7d)
                    .withTopP(1.0d)
                    .withMaxTokens(2048)
                    .build();

            // 创建消息数组
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的客服助手，请用专业、友好的方式回答用户的问题。"));

            // 如果有上下文消息，添加到消息列表
            if (chatAggregate.getContextMessages() != null) {
                for (String contextMessage : chatAggregate.getContextMessages()) {
                    messages.add(new UserMessage(contextMessage));
                }
            }

            // 添加当前用户消息
            messages.add(new UserMessage(chatAggregate.getContent()));

            // 创建Prompt对象
            Prompt prompt = new Prompt(messages, options);

            // 返回流式响应
            return tongYiChatModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && 
                            response.getResult().getOutput() != null && 
                            response.getResult().getOutput().getContent() != null) {
                            return response.getResult().getOutput().getContent();
                        }
                        return "";
                    })
                    .filter(content -> !content.isEmpty())
                    // 确保每个响应作为单独事件发送
                    .distinctUntilChanged()
                    // 添加延迟以确保前端能够正确处理
                    .delayElements(Duration.ofMillis(50));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理流式聊天请求时发生错误", e);
        }
    }

} 