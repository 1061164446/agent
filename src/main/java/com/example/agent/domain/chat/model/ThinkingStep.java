package com.example.agent.domain.chat.model;

/**
 * 思考步骤模型
 */
public class ThinkingStep {
    private StepType type;        // 步骤类型
    private String content;       // 步骤内容
    private Long timestamp;       // 时间戳
    private String sessionId;     // 会话ID

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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
} 