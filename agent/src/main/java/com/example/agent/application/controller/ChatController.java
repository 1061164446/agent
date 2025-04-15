package com.example.agent.application.controller;

import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

    /**
     * 构造函数
     * @param chatService 聊天服务
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
            return chatService.streamMessage(chatAggregate);
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
} 