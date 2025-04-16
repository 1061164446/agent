package com.example.agent.domain.chat.service;

import com.example.agent.domain.chat.model.ChatSession;
import com.example.agent.domain.chat.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天会话服务
 * 负责管理聊天会话的生命周期和状态
 */
@Service
public class ChatSessionService {
    private final Map<String, ChatSession> sessionStore = new ConcurrentHashMap<>();

    /**
     * 获取或创建会话
     * @param sessionId 会话ID
     * @return 会话对象
     */
    public ChatSession getOrCreateSession(String sessionId) {
        return sessionStore.computeIfAbsent(sessionId, ChatSession::new);
    }

    /**
     * 添加消息到会话
     * @param sessionId 会话ID
     * @param message 消息对象
     */
    public void addMessage(String sessionId, ChatMessage message) {
        ChatSession session = getOrCreateSession(sessionId);
        
        // 如果是用户消息，检查是否包含自我介绍
        if (message.getRole().equals("user")) {
            String content = message.getContent().trim();
            if (content.startsWith("我是")) {
                String userName = content.substring(2).trim();
                session.setUserName(userName);
                System.out.println("设置用户名: " + userName + " 到会话: " + sessionId);
            }
        }
        
        session.addMessage(message);
    }

    /**
     * 获取最近的聊天历史
     * @param sessionId 会话ID
     * @param count 获取的消息数量
     * @return 消息列表
     */
    public List<ChatMessage> getRecentHistory(String sessionId, int count) {
        ChatSession session = sessionStore.get(sessionId);
        if (session == null) {
            return new ArrayList<>();
        }
        return session.getRecentMessages(count);
    }

    /**
     * 获取用户名
     * @param sessionId 会话ID
     * @return 用户名，如果不存在则返回null
     */
    public String getUserName(String sessionId) {
        ChatSession session = sessionStore.get(sessionId);
        if (session != null) {
            String userName = session.getUserName();
            System.out.println("获取用户名: " + userName + " 从会话: " + sessionId);
            return userName;
        }
        return null;
    }
} 