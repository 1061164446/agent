package com.example.agent.domain.chat.service;

import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.chat.model.StepType;
import com.example.agent.domain.chat.model.ThinkingStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 思考服务
 */
@Service
public class ThinkingService {
    private final ChatService chatService;

    @Autowired
    public ThinkingService(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 生成思考步骤流
     * @param userInput 用户输入
     * @param sessionId 会话ID
     * @return 思考步骤流
     */
    public Flux<ThinkingStep> generateThinkingSteps(String userInput, String sessionId) {
        String thinkingPrompt = String.format(
            "在回答问题\"%s\"之前，请按照以下格式输出思考过程：\n" +
            "思考过程开始\n" +
            "1.分析用户意图\n" +
            "2.理解问题背景\n" +
            "3.搜索相关知识\n" +
            "4.组织答案框架\n" +
            "思考过程结束",
            userInput
        );

        ChatAggregate thinkingRequest = new ChatAggregate();
        thinkingRequest.setContent(thinkingPrompt);
        thinkingRequest.setSessionId(sessionId);
        
        return chatService.streamMessage(thinkingRequest)
            .filter(content -> content.trim().length() > 0)
            .map(content -> {
                ThinkingStep step = new ThinkingStep();
                step.setType(StepType.UNDERSTANDING);
                step.setContent(content.trim());
                step.setSessionId(sessionId);
                step.setTimestamp(System.currentTimeMillis());
                return step;
            })
            .filter(step -> step.getContent().length() > 0)
            .takeWhile(step -> !step.getContent().contains("思考过程结束"));
    }

    private boolean containsStepMarker(String content, String[] markers) {
        for (String marker : markers) {
            if (content.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private ThinkingStep createStep(String content, String sessionId) {
        ThinkingStep step = new ThinkingStep();
        step.setType(StepType.UNDERSTANDING);
        step.setContent(content);
        step.setSessionId(sessionId);
        step.setTimestamp(System.currentTimeMillis());
        return step;
    }
} 