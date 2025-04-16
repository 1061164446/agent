package com.example.agent.domain.chat.service;

import com.example.agent.domain.chat.model.ThinkingContext;
import com.example.agent.domain.chat.model.ThinkingStep;
import com.example.agent.domain.chat.model.StepType;
import com.example.agent.domain.chat.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.StringBuilder;
import java.time.Duration;

/**
 * 思考服务
 * 负责处理AI的思考过程和上下文分析，包括用户输入分析、关键词提取、问题领域识别等功能
 *
 * @author AI Assistant
 * @version 1.0
 * @since 2024-01-01
 */
@Service
public class ThinkingService {
    
    private final ChatSessionService chatSessionService;
    private final Map<String, ThinkingContext> contextStore = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param chatSessionService 聊天会话服务
     */
    @Autowired
    public ThinkingService(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * 分析用户输入并生成思考步骤
     *
     * @param sessionId 会话ID
     * @param userInput 用户输入内容
     * @return 思考步骤列表
     * @throws IllegalArgumentException 当sessionId或userInput为空时
     */
    public List<ThinkingStep> analyzeInput(String sessionId, String userInput) {
        if (sessionId == null || userInput == null) {
            throw new IllegalArgumentException("会话ID和用户输入不能为空");
        }
        
        ThinkingContext context = getOrCreateContext(sessionId, userInput);
        List<ThinkingStep> steps = new ArrayList<>();
        
        // 获取历史对话
        List<ChatMessage> recentHistory = chatSessionService.getRecentHistory(sessionId, 5);
        String history = buildHistoryString(recentHistory);
        steps.add(createStep(StepType.HISTORY_UPDATE, "历史对话：\n" + history));
        
        // 提取关键词
        List<String> keywords = extractKeywords(userInput);
        context.setKeywords(keywords);
        steps.add(createStep(StepType.KEYWORD_EXTRACTION, "提取关键词: " + String.join(", ", keywords)));
        
        // 识别问题领域
        String domain = identifyDomain(userInput, keywords);
        context.setDomain(domain);
        steps.add(createStep(StepType.DOMAIN_IDENTIFICATION, "问题领域: " + domain));
        
        // 识别问题类型
        String questionType = identifyQuestionType(userInput);
        context.setQuestionType(questionType);
        steps.add(createStep(StepType.QUESTION_TYPE, "问题类型: " + questionType));
        
        // 分析上下文
        String contextAnalysis = analyzeContext(history, userInput, keywords, domain, questionType);
        steps.add(createStep(StepType.CONTEXT_ANALYSIS, contextAnalysis));
        
        contextStore.put(sessionId, context);
        return steps;
    }

    /**
     * 构建历史对话字符串
     *
     * @param messages 历史消息列表
     * @return 格式化的历史对话字符串
     */
    private String buildHistoryString(List<ChatMessage> messages) {
        StringBuilder historyBuilder = new StringBuilder();
        for (ChatMessage message : messages) {
            historyBuilder.append(message.getRole())
                         .append(": ")
                         .append(message.getContent())
                         .append("\n");
        }
        return historyBuilder.toString();
    }

    /**
     * 分析上下文并生成分析报告
     *
     * @param history 历史对话内容
     * @param userInput 当前用户输入
     * @param keywords 提取的关键词列表
     * @param domain 识别的问题领域
     * @param questionType 识别的问题类型
     * @return 上下文分析报告
     */
    private String analyzeContext(String history, String userInput, List<String> keywords, 
                                String domain, String questionType) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("上下文分析：\n");
        analysis.append("1. 历史对话内容：\n").append(history).append("\n");
        analysis.append("2. 当前问题：").append(userInput).append("\n");
        analysis.append("3. 关键词：").append(String.join(", ", keywords)).append("\n");
        analysis.append("4. 问题领域：").append(domain).append("\n");
        analysis.append("5. 问题类型：").append(questionType).append("\n");
        return analysis.toString();
    }

    /**
     * 获取或创建思考上下文
     *
     * @param sessionId 会话ID
     * @param userInput 用户输入
     * @return 思考上下文对象
     */
    private ThinkingContext getOrCreateContext(String sessionId, String userInput) {
        return contextStore.computeIfAbsent(sessionId, 
            key -> new ThinkingContext(sessionId, userInput));
    }

    /**
     * 创建思考步骤
     *
     * @param type 步骤类型
     * @param content 步骤内容
     * @return 思考步骤对象
     */
    private ThinkingStep createStep(StepType type, String content) {
        ThinkingStep step = new ThinkingStep();
        step.setType(type);
        step.setContent(content);
        step.setTimestamp(System.currentTimeMillis());
        return step;
    }

    /**
     * 从输入文本中提取关键词
     *
     * @param input 输入文本
     * @return 关键词列表
     */
    private List<String> extractKeywords(String input) {
        List<String> keywords = new ArrayList<>();
        String[] words = input.split("\\s+");
        for (String word : words) {
            if (word.length() > 1 && !existCommonWord(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    /**
     * 识别输入文本的问题领域
     *
     * @param input 输入文本
     * @param keywords 关键词列表
     * @return 识别出的问题领域
     */
    private String identifyDomain(String input, List<String> keywords) {
        if (input.contains("代码") || input.contains("编程")) {
            return "编程";
        } else if (input.contains("数学") || input.contains("计算")) {
            return "数学";
        } else if (input.contains("天气") || input.contains("温度")) {
            return "天气";
        }
        return "通用";
    }

    /**
     * 识别问题类型
     *
     * @param input 输入文本
     * @return 识别出的问题类型
     */
    private String identifyQuestionType(String input) {
        if (input.contains("怎么") || input.contains("如何")) {
            return "方法指导";
        } else if (input.contains("为什么")) {
            return "原因解释";
        } else if (input.contains("什么") || input.contains("哪些")) {
            return "信息查询";
        }
        return "一般对话";
    }

    /**
     * 检查是否为常见词
     *
     * @param word 待检查的词
     * @return 如果是常见词返回true，否则返回false
     */
    private boolean existCommonWord(String word) {
        String[] commonWords = {
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", 
            "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", 
            "你", "会", "着", "没有", "看", "好", "自己", "这"
        };
        return Arrays.asList(commonWords).contains(word);
    }

    /**
     * 获取指定会话的思考上下文
     *
     * @param sessionId 会话ID
     * @return 思考上下文对象，如果不存在返回null
     */
    public ThinkingContext findContext(String sessionId) {
        return contextStore.get(sessionId);
    }

    /**
     * 清除指定会话的思考上下文
     *
     * @param sessionId 会话ID
     */
    public void clearContext(String sessionId) {
        contextStore.remove(sessionId);
    }

    /**
     * 生成思考步骤
     *
     * @param content 用户输入内容
     * @param sessionId 会话ID
     * @param history 历史消息列表
     * @return 思考步骤流
     */
    public Flux<ThinkingStep> generateThinkingSteps(String content, String sessionId, List<Message> history) {
        List<ThinkingStep> steps = analyzeInput(sessionId, content);
        return Flux.fromIterable(steps)
                  .delayElements(Duration.ofMillis(500)); // 添加延迟以模拟思考过程
    }
} 