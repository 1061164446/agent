package com.example.agent.domain.chat.service;


import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.chat.model.StepType;
import com.example.agent.domain.chat.model.ThinkingStep;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 思考服务
 * 负责处理AI的思考过程和上下文分析
 */
@Service
public class ThinkingService {

    @Autowired
    private ChatService chatService;;


    /**
     * 生成思考步骤
     * @param input 用户输入
     * @param sessionId 会话ID
     * @param history 历史消息
     * @return 思考步骤流
     */
    public Flux<ThinkingStep> generateThinkingSteps(String input, String sessionId, List<Message> history) {
        // 1. 构建更详细的思考提示词
        String thinkingPrompt = String.format(
                "在回答问题\"%s\"之前，请按照以下步骤进行思考：\n" +
                        "1. 用户意图分析：分析用户的核心诉求和目标\n" +
                        "2. 问题背景理解：理解问题的上下文和背景信息\n" +
                        "3. 相关知识搜索：列出需要用到的相关知识点\n" +
                        "4. 答案框架组织：规划回答的整体框架和结构\n" +
                        "\n请开始分析：",
                input
        );


        // 构建ChatAggregate对象
        ChatAggregate chatAggregate = new ChatAggregate();
        chatAggregate.setContent(thinkingPrompt);
        chatAggregate.setSessionId(sessionId);

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

        return chatService.processStreamMessage(chatAggregate)
                .filter(content -> content.trim().length() > 0)
                .map(content -> {
                    ThinkingStep step = new ThinkingStep();
                    step.setType(StepType.UNDERSTANDING);
                    step.setContent(content.trim());
                    step.setSessionId(sessionId);
                    step.setTimestamp(System.currentTimeMillis());
                    return step;
                })
                .filter(step -> step.getContent().length() > 0);
    }

} 