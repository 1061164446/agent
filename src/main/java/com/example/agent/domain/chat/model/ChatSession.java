package com.example.agent.domain.chat.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天会话实体
 */
public class ChatSession {
    private String sessionId;           // 会话ID
    private String userName;            // 用户名称
    private List<ChatMessage> messages; // 消息历史

    public ChatSession(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new ArrayList<>();
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
    }

    public List<ChatMessage> getRecentMessages(int count) {
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserName() {
        return userName;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
} 