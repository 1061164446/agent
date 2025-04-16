package com.example.agent.domain.chat.model;

import java.util.List;
import java.util.ArrayList;

/**
 * 思考上下文
 * 用于存储AI的思考过程和上下文信息
 */
public class ThinkingContext {
    private String sessionId;           // 会话ID
    private String userInput;           // 用户输入
    private List<String> keywords;      // 提取的关键词
    private String domain;              // 问题领域
    private String questionType;        // 问题类型
    private List<String> history;       // 历史对话
    private String summary;             // 对话摘要

    public ThinkingContext(String sessionId, String userInput) {
        this.sessionId = sessionId;
        this.userInput = userInput;
        this.keywords = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

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

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * 添加历史对话
     * @param message 对话消息
     */
    public void addHistory(String message) {
        this.history.add(message);
        // 如果历史记录超过10条，生成摘要
        if (this.history.size() >= 10) {
            generateSummary();
        }
    }

    /**
     * 生成对话摘要
     */
    private void generateSummary() {
        // TODO: 实现摘要生成逻辑
        this.summary = "对话摘要：" + String.join("; ", this.history.subList(0, 5));
    }
}

/**
 * 聊天消息实体
 */
class ChatMessage {
    private String role;      // 角色：user 或 assistant
    private String content;   // 消息内容
    private Long timestamp;   // 时间戳

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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