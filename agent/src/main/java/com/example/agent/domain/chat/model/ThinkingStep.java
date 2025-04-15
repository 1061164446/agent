package com.example.agent.domain.chat.model;

/**
 * 思考步骤实体类
 */
public class ThinkingStep {

    /**
     * 步骤类型
     */
    private StepType type;

    /**
     * 步骤内容
     */
    private String content;

    /**
     * 步骤顺序
     */
    private Integer order;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 创建时间戳
     */
    private Long timestamp;

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
} 