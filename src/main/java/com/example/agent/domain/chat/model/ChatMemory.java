package com.example.agent.domain.chat.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 聊天记忆模型
 * 用于存储和管理聊天历史
 */
public class ChatMemory {
    private String sessionId;           // 会话ID
    private List<ChatMessage> messages; // 消息历史
    private Map<String, Object> metadata; // 元数据
    private String summary;             // 对话摘要
    private int maxHistorySize;         // 最大历史记录数

    public ChatMemory(String sessionId) {
        this.sessionId = sessionId;
        this.messages = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.maxHistorySize = 20; // 默认保存最近20条消息
    }

    /**
     * 添加消息到记忆
     */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        // 如果超过最大历史记录数，移除最旧的消息
        if (messages.size() > maxHistorySize) {
            messages.remove(0);
        }
        // 更新摘要
        updateSummary();
    }

    /**
     * 获取最近的聊天历史
     */
    public List<ChatMessage> getRecentHistory(int count) {
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    /**
     * 获取完整的聊天历史
     */
    public List<ChatMessage> getFullHistory() {
        return new ArrayList<>(messages);
    }

    /**
     * 更新对话摘要
     */
    private void updateSummary() {
        // 简单的摘要生成逻辑
        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("对话摘要：\n");
        
        // 提取关键信息
        List<String> keyPoints = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message.getRole().equals("user")) {
                String content = message.getContent();
                if (content.startsWith("我是")) {
                    keyPoints.add("用户身份：" + content.substring(2));
                } else if (content.contains("？") || content.contains("?")) {
                    keyPoints.add("用户问题：" + content);
                }
            }
        }
        
        // 生成摘要
        if (!keyPoints.isEmpty()) {
            summaryBuilder.append(String.join("\n", keyPoints));
        } else {
            summaryBuilder.append("暂无关键信息");
        }
        
        this.summary = summaryBuilder.toString();
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public String getSummary() {
        return summary;
    }

    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }
} 