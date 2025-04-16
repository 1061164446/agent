<template>
    <div class="app-container">
        <!-- 左侧聊天记录列表 -->
        <div class="chat-list">
            <div class="chat-list-header">
                <h1>AI Assistant</h1>
                <button class="new-chat-btn" @click="startNewChat">
                    <span>开启新对话</span>
                </button>
            </div>
            <div class="chat-list-items">
                <div v-for="(chat, index) in chatHistory" 
                     :key="index"
                     :class="['chat-list-item', { active: currentChatId === chat.id }]"
                     @click="switchChat(chat.id)">
                    <span class="chat-icon">💬</span>
                    <span class="chat-title">对话 {{ chat.id }}</span>
                </div>
            </div>
        </div>

        <!-- 右侧聊天区域 -->
        <div class="chat-container">
            <div class="chat-messages" ref="messagesContainer">
                <div v-for="(message, index) in messages" 
                     :key="index"
                     :class="['message', message.role]">
                    <!-- AI消息（思考和回答）显示在左边 -->
                    <template v-if="message.role === 'thinking' || message.role === 'assistant'">
                        <div class="message-header" v-if="message.role === 'thinking'">
                            <div class="avatar">
                                <img :src="assistantAvatar" alt="AI Assistant">
                            </div>
                            <div class="message-status">
                                <span v-if="message.thinkingCompleted" class="completed-thinking">已通过深度思考（用时 {{ message.thinkingTime }}s）</span>
                                <span v-else class="thinking-in-progress">思考中...{{ message.thinkingTime ? `(${message.thinkingTime}s)` : '' }}</span>
                            </div>
                        </div>
                        <div class="message-content" :class="[message.role, { thinking: message.isThinking, error: message.isError }]">
                            <div v-if="message.isThinking" class="thinking-dots">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                            <div v-else v-html="formatMarkdown(message.content)"></div>
                        </div>
                    </template>
                    <!-- 用户消息显示在右边 -->
                    <template v-else-if="message.role === 'user'">
                        <div class="message-content">
                            {{ message.content }}
                        </div>
                    </template>
                </div>
            </div>
            <div class="chat-input-container">
                <div class="input-wrapper">
                    <textarea
                        v-model="userInput"
                        @keydown="handleKeyDown"
                        @focus="handleInputFocus"
                        placeholder="给 AI Assistant 发送消息..."
                        rows="1"
                        ref="inputArea"
                    ></textarea>
                    <div class="input-actions">
                        <button class="send-button" @click="sendMessage" :disabled="isProcessing">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="22" y1="2" x2="11" y2="13"></line>
                                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                            </svg>
                        </button>
                    </div>
                </div>
                <div class="input-tips">
                    <div class="input-tip-item">
                        <span class="shortcut">Enter</span>
                        <span>发送</span>
                    </div>
                    <div class="input-tip-item">
                        <span class="shortcut">Shift</span> + <span class="shortcut">Enter</span>
                        <span>换行</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { ChatService } from '../services/chatService';
import type { Message } from '../types/chat';
import { marked, type MarkedOptions } from 'marked';
import hljs from 'highlight.js/lib/core';
import 'highlight.js/styles/github.css';

// 注册常用语言支持
import javascript from 'highlight.js/lib/languages/javascript';
import typescript from 'highlight.js/lib/languages/typescript';
import python from 'highlight.js/lib/languages/python';
import java from 'highlight.js/lib/languages/java';
import cpp from 'highlight.js/lib/languages/cpp';
import xml from 'highlight.js/lib/languages/xml';
import json from 'highlight.js/lib/languages/json';
import bash from 'highlight.js/lib/languages/bash';
import sql from 'highlight.js/lib/languages/sql';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('python', python);
hljs.registerLanguage('java', java);
hljs.registerLanguage('cpp', cpp);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('json', json);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('sql', sql);

// 添加类型声明
declare global {
    interface Window {
        _typingTimerId?: ReturnType<typeof setTimeout>;
        _typingState?: {
            fullContent: string;
            currentIndex: number;
            element: Message | null;
        };
    }
}

// 拓展消息类型，添加思考时间
declare module '../types/chat' {
    interface Message {
        thinkingTime?: number;
        thinkingCompleted?: boolean;
        isThinking?: boolean;
        isError?: boolean;
    }
}

const assistantIcon = `
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#0051a2" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
</svg>
`;

const assistantAvatar = URL.createObjectURL(new Blob([assistantIcon], { type: 'image/svg+xml' }));

const chatService = new ChatService();
const messages = ref<Message[]>([
    {
        role: 'assistant',
        content: '我是 AI Assistant, 很高兴见到你!\n我可以帮你写代码、读文件、写作和创建内容，请把你的任务交给我吧~',
        type: 'text',
        timestamp: Date.now()
    }
]);
const userInput = ref('');
const isProcessing = ref(false);
const messagesContainer = ref<HTMLElement | null>(null);
const inputArea = ref<HTMLTextAreaElement | null>(null);
const currentChatId = ref<string>('1');
const chatHistory = ref([{ id: '1', title: '对话 1' }]);

// 配置 marked
marked.setOptions({
    highlight: function(code: string, lang: string) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return hljs.highlight(code, lang).value;
            } catch (e) {
                console.error(e);
            }
        }
        return code;
    },
    breaks: true,
    gfm: true
} as MarkedOptions);

// 渲染 Markdown
const renderMarkdown = (text: string) => {
    return marked(text)
}

const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
        if (!e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    }
};

const handleInputFocus = (e: FocusEvent) => {
    // 确保滚动到最新消息
    nextTick(() => scrollToBottom());
};

const sendMessage = async () => {
    if (!userInput.value.trim() || isProcessing.value) return;

    const userMessage = userInput.value.trim();
    
    // 防止重复添加相同的用户消息
    const hasIdenticalUserMessage = messages.value.some(
        msg => msg.role === 'user' && 
              msg.content === userMessage &&
              Date.now() - (msg.timestamp || 0) < 10000 // 10秒内的相同消息视为重复
    );
    
    if (!hasIdenticalUserMessage) {
        messages.value.push({
            role: 'user',
            content: userMessage,
            type: 'text',
            timestamp: Date.now()
        });
    } else {
        console.log('防止重复添加相同的用户消息');
        return; // 如果是重复消息，直接返回不处理
    }

    userInput.value = '';
    isProcessing.value = true;
    
    // 声明在外部作用域，使finally块可以访问
    let thinkingTimerId: ReturnType<typeof setTimeout> | null = null;

    try {
        const reader = await chatService.streamMessageWithThinking(userMessage);
        let lastContent = '';
        let isThinkingPhase = true;
        let currentResponse: Message | null = null;
        let accumulatedContent = '';
        let thinkingStartTime = Date.now();
        let lastUpdateTime = Date.now();
        
        // 添加思考计时器函数
        const updateThinkingTime = () => {
            // 找到所有思考消息
            messages.value.forEach(msg => {
                if (msg.role === 'thinking') {
                    // 更新思考时间
                    msg.thinkingTime = Math.floor((Date.now() - thinkingStartTime) / 1000);
                }
            });
            
            // 如果仍在思考中，继续计时
            if (isThinkingPhase) {
                thinkingTimerId = setTimeout(updateThinkingTime, 1000);
            }
        };
        
        // 启动思考计时器
        updateThinkingTime();

        console.log('开始处理流式响应');
        
        await chatService.processStream(reader, async (response) => {
            if (response.type === 'thinking') {
                const newContent = response.content.trim();
                
                if (newContent === lastContent) {
                    return;
                }

                console.log('收到思考步骤:', newContent.substring(0, 30) + '...');

                // 检查是否是新的思考步骤（通过数字开头的格式）
                if (/^\d+\./.test(newContent) || !lastContent) {
                    messages.value.push({
                        role: 'thinking' as const,
                        content: newContent,
                        type: 'thinking' as const,
                        timestamp: Date.now(),
                        thinkingTime: Math.floor((Date.now() - thinkingStartTime) / 1000)
                    });
                } else {
                    const lastMessage = messages.value[messages.value.length - 1];
                    if (lastMessage.role === 'thinking') {
                        lastMessage.content = newContent;
                    }
                }
                
                lastContent = newContent;
            } else if (response.type === 'response') {
                // 当收到第一个response类型消息时，标记思考阶段结束
                if (isThinkingPhase) {
                    isThinkingPhase = false;
                    // 更新所有思考消息的状态
                    messages.value.forEach(msg => {
                        if (msg.role === 'thinking') {
                            msg.thinkingCompleted = true;
                            msg.thinkingTime = Math.floor((Date.now() - thinkingStartTime) / 1000);
                        }
                    });
                }

                const newContent = response.content.trim();
                
                if (!currentResponse) {
                    currentResponse = {
                        role: 'assistant' as const,
                        content: newContent,
                        type: 'text' as const,
                        timestamp: Date.now()
                    };
                    messages.value.push(currentResponse);
                } else {
                    currentResponse.content = newContent;
                }
            }
            
            await nextTick(() => scrollToBottom());
        });
        
        console.log('流式处理完成');
    } catch (error) {
        console.error('Error:', error);
        messages.value.push({
            role: 'assistant',
            content: '抱歉，发生了错误，请稍后重试。',
            type: 'text',
            timestamp: Date.now()
        });
    } finally {
        if (thinkingTimerId !== null) {
            clearTimeout(thinkingTimerId);
            thinkingTimerId = null;
        }
        
        isProcessing.value = false;
        scrollToBottom();
    }
};

const scrollToBottom = () => {
    if (messagesContainer.value) {
        const scrollOptions = {
            top: messagesContainer.value.scrollHeight,
            behavior: 'smooth' as const
        };
        messagesContainer.value.scrollTo(scrollOptions);
    }
};

const startNewChat = () => {
    const newId = Date.now().toString();
    chatHistory.value.unshift({ id: newId, title: `对话 ${newId}` });
    currentChatId.value = newId;
    messages.value = [];
};

const switchChat = (chatId: string) => {
    currentChatId.value = chatId;
};

// 添加时间格式化函数
const formatTime = (timestamp: number | undefined) => {
    if (!timestamp) {
        return '';
    }
    const date = new Date(timestamp);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
};

// 定义formatMarkdown函数
const formatMarkdown = (text: string): string => {
    // 配置marked选项
    marked.setOptions({
        breaks: true,        // 将\n转换为<br>
        gfm: true            // 启用GitHub风格Markdown
    });
    
    try {
        // 解析Markdown文本并确保返回字符串
        return marked.parse(text) as string;
    } catch (error) {
        console.error('Markdown解析错误:', error);
        return text; // 如果解析失败，返回原始文本
    }
};

// 添加调试函数
const logMessageState = () => {
    console.log('==== 当前消息状态 ====');
    console.log('消息总数:', messages.value.length);
    console.log('思考消息数:', messages.value.filter(msg => msg.role === 'thinking').length);
    console.log('已完成思考消息数:', messages.value.filter(msg => msg.role === 'thinking' && msg.thinkingCompleted).length);
    console.log('详细信息:', messages.value.map(msg => ({
        role: msg.role,
        type: msg.type,
        completed: msg.thinkingCompleted,
        time: msg.thinkingTime,
        content: msg.content.substring(0, 30) + (msg.content.length > 30 ? '...' : '')
    })));
    console.log('=====================');
};

onMounted(() => {
    scrollToBottom();
});
</script>

<style scoped>
.app-container {
    display: flex;
    width: 100%;
    height: 100vh;
    background: #ffffff;
    margin: 0;
    padding: 0;
    overflow: hidden;
}

.chat-list {
    width: 260px;
    display: flex;
    flex-direction: column;
    background: #F5F5F5;  /* 苹果侧边栏灰色 */
    border: none;
}

.chat-list-header {
    padding: 16px;
    background: #F5F5F5;
    border: none;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.chat-list-header h1 {
    margin: 0;
    font-size: 22px;
    font-weight: 600;
    color: #1d1d1f;
    letter-spacing: -0.5px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.chat-list-header h1::before {
    content: "";
    display: inline-block;
    width: 24px;
    height: 24px;
    background: #0071e3;
    border-radius: 6px;
    margin-right: 4px;
}

.new-chat-btn {
    width: 100%;
    padding: 8px 12px;
    background: #007AFF;
    border: none;
    border-radius: 10px;
    color: #ffffff;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
}

.chat-list-items {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    background: #F5F5F5;
}

.chat-list-item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 4px;
    color: #1d1d1f;
    font-size: 14px;
    font-weight: 500;
}

.chat-list-item.active {
    background: rgba(0, 122, 255, 0.1);  /* 苹果蓝的透明版本 */
    color: #007AFF;
}

.chat-icon {
    margin-right: 10px;
    font-size: 16px;
}

.chat-title {
    font-size: 14px;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.chat-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: #ffffff;
    border: none;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px 0;
    background: #ffffff;
}

.message {
    padding: 0 15%;
    margin-bottom: 24px;
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
}

.message.user {
    align-items: flex-end;
}

.message.user .message-content {
    background: #007AFF;  /* 苹果系统蓝 */
    color: #ffffff;
    border-radius: 20px 20px 4px 20px;
    max-width: 80%;
    position: relative;
    padding: 12px 16px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.assistant,
.message.thinking {
    align-items: flex-start;
}

.message.assistant .message-content {
    background: #E9E9EB;  /* 回答消息使用系统灰色 */
    color: #000000;
    border-radius: 20px 20px 20px 4px;
    max-width: 80%;
    position: relative;
    padding: 12px 16px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.thinking .message-content {
    background: #F0F7FF;  /* 思考消息使用浅蓝色背景 */
    color: #666666;
    font-size: 14px;
    border-radius: 20px 20px 20px 4px;
    max-width: 80%;
    position: relative;
    padding: 12px 16px;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.thinking .message-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 8px;
    color: #007AFF;  /* 使用苹果蓝色 */
    font-size: 13px;
}

.message.thinking {
    opacity: 0.9;
    margin-bottom: 16px;
}

.message-content {
    position: relative;
    padding: 12px 16px;
    border-radius: 12px;
    line-height: 1.8;
}

.message.thinking {
    background: transparent;
    padding: 0 15%;
    margin-bottom: 12px;
}

.chat-input-container {
    padding: 24px 15%;
    background: #ffffff;
    border: none;
    position: relative;
}

.input-wrapper {
    position: relative;
    border: none;
    border-radius: 20px;
    background: #E9E9EB;
}

.input-tips {
    position: absolute;
    left: 0;
    bottom: -24px;
    font-size: 12px;
    color: #86868b;
    display: flex;
    align-items: center;
    gap: 16px;
}

.input-tip-item {
    display: flex;
    align-items: center;
    gap: 4px;
}

.input-tip-item .shortcut {
    color: #666666;
    background: #f5f5f7;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 11px;
}

textarea {
    width: 100%;
    padding: 20px 60px 20px 20px;
    min-height: 80px;
    border: none;
    resize: none;
    font-size: 16px;
    line-height: 1.6;
    color: #1d1d1f;
    background: transparent;
}

textarea:focus {
    outline: none;
}

textarea::placeholder {
    color: #86868b;
}

.input-actions {
    position: absolute;
    right: 16px;
    bottom: 16px;
    display: flex;
    align-items: center;
}

.send-button {
    padding: 10px;
    background: #007AFF;
    border: none;
    border-radius: 15px;
    color: #ffffff;
    cursor: pointer;
    margin-right: 4px;
}

.send-button:disabled {
    background: #cccccc;
    color: #666666;
    cursor: not-allowed;
}

.send-button svg {
    width: 18px;
    height: 18px;
}

/* 代码块样式 */
.message-content.assistant :deep(pre) {
    background: #f6f8fa;
    padding: 16px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 8px 0;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 14px;
    line-height: 1.45;
    border: 1px solid #e1e4e8;
}

.message-content.assistant :deep(code) {
    background: #f6f8fa;
    padding: 0.2em 0.4em;
    border-radius: 3px;
    font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
    font-size: 14px;
}

/* Markdown 样式 */
.message-content.assistant :deep(h1),
.message-content.assistant :deep(h2),
.message-content.assistant :deep(h3),
.message-content.assistant :deep(h4),
.message-content.assistant :deep(h5),
.message-content.assistant :deep(h6) {
    margin: 1em 0 0.5em;
    font-weight: 600;
    line-height: 1.25;
}

.message-content.assistant :deep(p) {
    margin: 0.5em 0;
    line-height: 1.6;
}

.message-content.assistant :deep(ul),
.message-content.assistant :deep(ol) {
    padding-left: 2em;
    margin: 0.5em 0;
}

.message-content.assistant :deep(li) {
    margin: 0.25em 0;
}

.message-content.assistant :deep(blockquote) {
    border-left: 4px solid #0051a2;
    padding-left: 1em;
    margin: 0.5em 0;
    color: #666;
}

.message-content.assistant :deep(a) {
    color: #0051a2;
    text-decoration: none;
}

.message-content.assistant :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 0.5em 0;
}

.message-content.assistant :deep(th),
.message-content.assistant :deep(td) {
    border: 1px solid #dfe2e5;
    padding: 6px 13px;
}

.message-content.assistant :deep(th) {
    background: #f6f8fa;
}

/* 基础样式 */
.chat-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #f5f7fa;
    padding: 20px;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

/* 思考点样式 */
.thinking-dots {
    display: flex;
    gap: 6px;
    align-items: center;
    padding: 4px;
}

.thinking-dots span {
    width: 8px;
    height: 8px;
    background: #1890ff;
    border-radius: 50%;
}

/* 响应式调整 */
@media (max-width: 768px) {
    .message.thinking {
        padding: 16px;
        margin-bottom: 10px;
    }
    
    .message.thinking .message-header {
        gap: 12px;
        margin-bottom: 12px;
    }
    
    .message.thinking .avatar {
        width: 32px;
        height: 32px;
    }
    
    .message.thinking .message-content {
        padding: 12px;
        font-size: 13px;
    }
}
</style> 