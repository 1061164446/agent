package com.example.agent.domain.chat.service;

import com.example.agent.domain.chat.model.ChatMemory;
import com.example.agent.domain.chat.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天记忆服务
 * 负责管理聊天记忆
 */
@Service
public class ChatMemoryService {
    private final Map<String, ChatMemory> memoryStore = new ConcurrentHashMap<>();

    /**
     * 获取或创建聊天记忆
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        return memoryStore.computeIfAbsent(sessionId, ChatMemory::new);
    }

    /**
     * 添加消息到记忆
     */
    public void addMessage(String sessionId, ChatMessage message) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.addMessage(message);
    }

    /**
     * 获取最近的聊天历史
     */
    public List<ChatMessage> getRecentHistory(String sessionId, int count) {
        ChatMemory memory = memoryStore.get(sessionId);
        if (memory == null) {
            return List.of();
        }
        return memory.getRecentHistory(count);
    }

    /**
     * 获取对话摘要
     */
    public String getSummary(String sessionId) {
        ChatMemory memory = memoryStore.get(sessionId);
        if (memory == null) {
            return "暂无对话摘要";
        }
        return memory.getSummary();
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String sessionId, String key, Object value) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.addMetadata(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String sessionId, String key) {
        ChatMemory memory = memoryStore.get(sessionId);
        if (memory == null) {
            return null;
        }
        return memory.getMetadata(key);
    }

    /**
     * 清除聊天记忆
     */
    public void clearMemory(String sessionId) {
        memoryStore.remove(sessionId);
    }

    /**
     * 设置最大历史记录数
     */
    public void setMaxHistorySize(String sessionId, int size) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.setMaxHistorySize(size);
    }
} 