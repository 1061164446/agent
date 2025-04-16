package com.example.agent.api.controller;


import com.example.agent.application.exception.BusinessException;
import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.chat.service.ThinkingService;
import org.springframework.ai.chat.memory.ChatMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * 聊天控制器
 * 处理聊天相关的HTTP请求
 * 支持以下功能：
 * 1. 文本对话
 * 2. 流式对话
 * 3. 多轮对话（支持上下文）
 * 4. 函数调用
 * 5. 图片生成
 * 6. 语音识别
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ThinkingService thinkingService;
    private final ObjectMapper objectMapper;
    private final ChatMemory chatMemory;

    @Autowired
    public ChatController(ChatService chatService, ThinkingService thinkingService, 
                         ObjectMapper objectMapper, ChatMemory chatMemory) {
        this.chatService = chatService;
        this.thinkingService = thinkingService;
        this.objectMapper = objectMapper;
        this.chatMemory = chatMemory;
    }

    /**
     * first 流式 AI 回答
     * @param request
     * @return
     */
    @PostMapping(value = "/send/thinking", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> sendMessage(@RequestBody ChatRequest request) {
        String content = request.getContent();
        String conversationId = request.getSessionId();
        
        // 创建用户消息并添加到记忆
        UserMessage userMessage = new UserMessage(content);
        chatMemory.add(conversationId, userMessage);
        
        // 获取历史消息
        List<Message> history = chatMemory.get(conversationId, 100);
        
        // 首先返回思考步骤，传入历史消息
        Flux<String> thinkingSteps = thinkingService.generateThinkingSteps(content, conversationId, history)
            .map(step -> {
                try {

                    // 确保每个思考步骤都作为独立的消息发送
                    System.out.println("Sending thinking step: " + step.getContent());
                    ChatResponse response = new ChatResponse("thinking", step.getContent());
                    return objectMapper.writeValueAsString(response) + "\n";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            })
            .filter(stepContent -> !stepContent.isEmpty())
            .doOnNext(stepContent -> System.out.println("思考步骤流: " + stepContent));


        // 构建ChatAggregate对象
        ChatAggregate chatAggregate = new ChatAggregate();
        chatAggregate.setContent(content);
        chatAggregate.setSessionId(conversationId);
        
        // 添加历史消息作为上下文
        String[] contextMessages = history.stream()
            .map(msg -> {
                if (msg instanceof UserMessage) {
                    return "user: " + msg.getContent();
                } else if (msg instanceof AssistantMessage) {
                    return "assistant: " + msg.getContent();
                }
                return "";
            })
            .filter(msg -> !msg.isEmpty())
            .toArray(String[]::new);
        chatAggregate.setContextMessages(contextMessages);

        // 使用现有的chat服务获取AI回答
        Flux<String> aiResponse = chatService.processStreamMessage(chatAggregate)
            .map(aiContent -> {
                try {
                    if (!aiContent.startsWith("思考过程开始") && !aiContent.contains("思考过程结束")) {
                        // 添加AI响应到记忆
                        AssistantMessage assistantMessage = new AssistantMessage(aiContent);
                        chatMemory.add(conversationId, assistantMessage);
                        
                        // 返回响应
                        ChatResponse response = new ChatResponse("response", aiContent);
                        return objectMapper.writeValueAsString(response) + "\n";
                    }
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            })
            .filter(responseStr -> !responseStr.isEmpty())
            .doOnNext(responseStr -> System.out.println("AI回答流: " + responseStr));

        return Flux.concat(thinkingSteps, aiResponse)
            .delayElements(Duration.ofMillis(50))
            .doOnError(e -> System.err.println("思考和回答流错误: " + e.getMessage()));
    }
}

/**
 * 聊天请求对象
 */
class ChatRequest {
    private String sessionId;
    private String content;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

/**
 * 聊天响应对象
 */
class ChatResponse {
    private String type;    // "thinking" 或 "response"
    private String content;
    private Long timestamp;

    public ChatResponse(String type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
} 