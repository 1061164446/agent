package com.example.agent.application.controller;

import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.exception.BusinessException;
import com.example.agent.domain.chat.service.ThinkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

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

    @Autowired
    public ChatController(ChatService chatService, ThinkingService thinkingService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.thinkingService = thinkingService;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送单轮文本对话
     * @param chatAggregate 聊天聚合对象
     * @return 聊天响应
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @PostMapping("/send")
    public ResponseEntity<ChatAggregate> sendMessage(@RequestBody ChatAggregate chatAggregate) {
        try {
            ChatAggregate response = chatService.chat(chatAggregate);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理聊天请求时发生错误", e);
        }
    }

    /**
     * 发送流式文本对话
     * @param chatAggregate 聊天聚合对象
     * @return 聊天响应流
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestBody ChatAggregate chatAggregate) {
        try {
            // 返回流式响应，确保每个响应都被发送为一个完整的SSE事件
            return chatService.streamMessage(chatAggregate)
                .map(chunk -> "data: " + chunk + "\n\n")
                .doOnError(e -> System.err.println("流式输出错误: " + e.getMessage()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理流式聊天请求时发生错误", e);
        }
    }

    /**
     * 发送多轮对话（支持上下文）
     * @param chatAggregate 聊天聚合对象
     * @return 聊天响应
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @PostMapping("/conversation")
    public ResponseEntity<ChatAggregate> sendConversation(@RequestBody ChatAggregate chatAggregate) {
        try {
            ChatAggregate response = chatService.chat(chatAggregate);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理多轮对话请求时发生错误", e);
        }
    }

    /**
     * 调用函数
     * @param chatAggregate 聊天聚合对象
     * @return 函数执行结果
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @PostMapping("/function")
    public ResponseEntity<ChatAggregate> callFunction(@RequestBody ChatAggregate chatAggregate) {
        try {
            chatAggregate.setFunctionType("function");
            ChatAggregate response = chatService.chat(chatAggregate);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理函数调用请求时发生错误", e);
        }
    }

    /**
     * 生成图片
     * @param chatAggregate 聊天聚合对象
     * @return 图片URL
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @PostMapping("/image")
    public ResponseEntity<ChatAggregate> generateImage(@RequestBody ChatAggregate chatAggregate) {
        try {
            ChatAggregate response = chatService.generateImage(chatAggregate);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理图片生成请求时发生错误", e);
        }
    }

    /**
     * 语音识别
     * @param chatAggregate 聊天聚合对象
     * @return 识别结果
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @PostMapping("/speech")
    public ResponseEntity<ChatAggregate> recognizeSpeech(@RequestBody ChatAggregate chatAggregate) {
        try {
            chatAggregate.setFunctionType("speech");
            ChatAggregate response = chatService.chat(chatAggregate);
            return ResponseEntity.ok(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理语音识别请求时发生错误", e);
        }
    }

    /**
     * 处理聊天消息并返回思考过程和回答
     * @param message 用户消息
     * @return 思考步骤流
     */
    @PostMapping(value = "/send/thinking", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> sendMessage(@RequestBody ChatRequest message) {
        String sessionId = UUID.randomUUID().toString();
        
        // 首先返回思考步骤
        Flux<String> thinkingSteps = thinkingService.generateThinkingSteps(message.getContent(), sessionId)
            .map(step -> {
                try {
                    // 确保每个思考步骤都作为独立的消息发送
                    System.out.println("Sending thinking step: " + step.getContent()); // 添加日志
                    ChatResponse response = new ChatResponse("thinking", step.getContent());
                    return objectMapper.writeValueAsString(response) + "\n";
                } catch (Exception e) {
                    e.printStackTrace(); // 添加错误日志
                    return "";
                }
            })
            .filter(content -> !content.isEmpty()) // 过滤掉空内容
            .doOnNext(content -> System.out.println("思考步骤流: " + content)); // 调试日志

        // 获取实际的AI回答
        ChatAggregate chatAggregate = new ChatAggregate();
        chatAggregate.setContent(message.getContent());
        chatAggregate.setSessionId(sessionId);

        // 使用现有的chat服务获取AI回答
        Flux<String> aiResponse = chatService.streamMessage(chatAggregate)
            .map(content -> {
                try {
                    // 确保这不是思考过程的内容
                    if (!content.startsWith("思考过程开始") && !content.contains("思考过程结束")) {
                        ChatResponse response = new ChatResponse("response", content);
                        return objectMapper.writeValueAsString(response) + "\n";
                    }
                    return "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            })
            .filter(content -> !content.isEmpty())
            .doOnNext(content -> System.out.println("AI回答流: " + content)); // 调试日志

        // 使用concat确保思考步骤在AI回答之前完成
        return Flux.concat(thinkingSteps, aiResponse)
            // 添加延迟确保前端能够正确处理
            .delayElements(Duration.ofMillis(50))
            .doOnError(e -> System.err.println("思考和回答流错误: " + e.getMessage()));
    }
}

/**
 * 聊天请求对象
 */
class ChatRequest {
    private String content;

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