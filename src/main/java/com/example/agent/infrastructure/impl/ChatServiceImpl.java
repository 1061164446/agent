package com.example.agent.infrastructure.impl;

import com.example.agent.domain.ChatAggregate;
import com.example.agent.domain.Message;
import com.example.agent.domain.Prompt;
import com.example.agent.domain.SystemMessage;
import com.example.agent.domain.TongYiChatOptions;
import com.example.agent.domain.UserMessage;
import com.example.agent.exception.BusinessException;
import com.example.agent.infrastructure.ChatService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

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
            
            // 添加系统提示词，包含会话上下文
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是一个专业的客服助手，请用专业、友好的方式回答用户的问题。");
            systemPrompt.append("\n\n重要提示：");
            systemPrompt.append("\n1. 请记住用户的身份信息，并在后续对话中保持连贯性。");
            systemPrompt.append("\n2. 如果用户提到过自己的名字或身份，请记住并在后续对话中使用。");
            systemPrompt.append("\n3. 保持对话的连贯性和上下文理解。");
            
            // 如果有上下文消息，添加到系统提示词中
            if (chatAggregate.getContextMessages() != null && chatAggregate.getContextMessages().length > 0) {
                systemPrompt.append("\n\n以下是本次对话的上下文信息：\n");
                for (String contextMessage : chatAggregate.getContextMessages()) {
                    systemPrompt.append(contextMessage).append("\n");
                }
                systemPrompt.append("\n请根据以上上下文信息，保持对话的连贯性，并记住用户的身份信息。");
            }
            
            messages.add(new SystemMessage(systemPrompt.toString()));

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