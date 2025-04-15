package com.example.agent.domain.chat.model;

import java.util.List;

/**
 * 思考上下文
 */
public class ThinkingContext {
    private String userInput;           // 用户输入
    private List<String> keywords;      // 提取的关键词
    private String domain;              // 问题领域
    private String questionType;        // 问题类型
    private String sessionId;           // 会话ID

    public ThinkingContext(String userInput, String sessionId) {
        this.userInput = userInput;
        this.sessionId = sessionId;
    }

    // Getters and Setters
    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
} 