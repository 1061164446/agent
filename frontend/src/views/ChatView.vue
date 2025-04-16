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
                    <div class="message-wrapper" :class="{ 'user-message': message.role === 'user' }">
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
                    </div>
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
const isTypingInProgress = ref(false);
const shouldStopTyping = ref(false);

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
    // 设置标志以停止所有打字效果
    shouldStopTyping.value = true;
    console.log('输入框获得焦点，停止打字效果');
    
    // 如果存在正在进行的打字效果，立即完成
    if (window._typingState) {
        const state = window._typingState;
        
        // 清除打字计时器
        if (window._typingTimerId) {
            clearTimeout(window._typingTimerId);
            window._typingTimerId = undefined;
        }
        
        // 确保所有内容立即显示
        if (state.element && state.fullContent) {
            console.log('立即显示完整内容:', state.fullContent.substring(0, 30) + '...');
            state.element.content = state.fullContent;
        }
    }
    
    // 重置打字状态
    isTypingInProgress.value = false;
    
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
            // 确保频繁更新UI不会导致性能问题
            const now = Date.now();
            const timeSinceLastUpdate = now - lastUpdateTime;
            
            // 对于长消息，限制更新频率
            if (currentResponse && timeSinceLastUpdate < 50 && accumulatedContent.length > 1000) {
                return; // 跳过此次更新
            }
            
            lastUpdateTime = now;

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
                    accumulatedContent = newContent;
                } else {
                    if (newContent !== accumulatedContent) {
                        const addedContent = newContent.slice(accumulatedContent.length);
                        if (addedContent) {
                            currentResponse.content = newContent;
                            accumulatedContent = newContent;
                            await nextTick();
                            messages.value = [...messages.value];
                        }
                    }
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

// 修改打字效果函数，检查是否应该停止
const typeNextChar = (state: any) => {
    // 检查是否应该停止打字
    if (shouldStopTyping.value || !state || !state.element) {
        isTypingInProgress.value = false;
        return;
    }
    
    if (state.currentIndex < state.fullContent.length) {
        // 添加下一个字符
        state.element.content = state.fullContent.substring(0, state.currentIndex + 1);
        state.currentIndex++;
        
        // 计算延迟
        let delay = 30;
        const nextChar = state.fullContent[state.currentIndex];
        if (nextChar && /[，。！？、；：""''（）【】《》]/.test(nextChar)) {
            delay = 120; // 标点符号停顿更长
        }
        
        // 继续打字
        window._typingTimerId = setTimeout(() => typeNextChar(state), delay);
        
        // 更新滚动位置
        nextTick(() => scrollToBottom());
    } else {
        // 打字效果完成
        isTypingInProgress.value = false;
    }
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
    background: #ebebeb;
    border: none;
}

.chat-list-header {
    padding: 16px;
    background: #ebebeb;
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
    background: #0051a2;  /* 加深新对话按钮颜色 */
    border: none;
    border-radius: 8px;
    color: #ffffff;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s ease;
}

.new-chat-btn:hover {
    background: #003d7a;  /* 加深新对话按钮悬停颜色 */
    transform: scale(1.01);
}

.chat-list-items {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    background: #ebebeb;
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
    transition: all 0.2s ease;
}

.chat-list-item:hover {
    background: rgba(0, 0, 0, 0.08);  /* 加深悬停效果 */
}

.chat-list-item.active {
    background: rgba(0, 113, 227, 0.15);  /* 加深选中状态背景 */
    color: #0051a2;  /* 加深选中状态文字颜色 */
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

.chat-container::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(to right, transparent, rgba(0, 81, 162, 0.1), transparent);
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px 0;
    background: #ffffff;
    scroll-behavior: smooth;
}

/* 自定义滚动条样式 */
.chat-messages::-webkit-scrollbar {
    width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
    background: transparent;
    border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
    background: rgba(0, 81, 162, 0.2);
    border-radius: 4px;
    transition: background 0.3s ease;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
    background: rgba(0, 81, 162, 0.4);
}

/* 添加滚动条阴影效果 */
.chat-messages {
    position: relative;
}

.chat-messages::after {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 8px;
    height: 100%;
    background: linear-gradient(to right, rgba(0, 0, 0, 0.05), transparent);
    pointer-events: none;
}

.message {
    padding: 0 15%;
    margin-bottom: 24px;
    position: relative;
    display: flex;
    flex-direction: column;
}

.message-wrapper {
    max-width: 100%;
    display: flex;
    flex-direction: column;
}

.message-wrapper.user-message {
    display: flex;
    justify-content: flex-end;
    align-items: flex-end;
}

.message-wrapper.user-message .message-content {
    background: #0051a2;
    color: #ffffff;
    padding: 12px 16px;
    border-radius: 12px 12px 2px 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
    max-width: 80%;
    margin-left: auto;
}

.message-header {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
}

.avatar {
    width: 32px;
    height: 32px;
    margin-right: 12px;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f5f7;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.message-status {
    font-size: 13px;
    color: #666666;
    margin-left: 8px;
}

.message-status .completed-thinking {
    color: #0051a2;
    font-weight: 500;
}

.message-status .thinking-in-progress {
    color: #6b7280;
}

.message-content {
    position: relative;
    margin-left: 44px;
    max-width: 80%;
    padding: 12px 16px;
    border-radius: 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    line-height: 1.8;
}

.message-content.user {
    color: #ffffff;
    background: #0051a2;
    padding: 12px 16px;
    border-radius: 12px 12px 2px 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
    max-width: 80%;
    margin-left: auto;
}

.message-content.assistant {
    background: #ffffff;
    color: #000000;
    padding: 12px 16px;
    border-radius: 12px 12px 12px 2px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
    max-width: 80%;
    margin-left: 44px;
    position: relative;
}

.message-content.assistant strong {
    font-weight: 600;
    color: #000000;
}

.message-content.assistant code {
    background: #f5f5f7;
    padding: 2px 4px;
    border-radius: 4px;
    font-family: monospace;
    font-size: 0.9em;
}

.message-content.assistant pre {
    background: #f5f5f7;
    padding: 12px;
    border-radius: 6px;
    overflow: auto;
    margin: 8px 0;
}

.message-content.assistant pre code {
    background: transparent;
    padding: 0;
}

.message-content.thinking {
    background: #f8f9fa;
    color: #666666;
    font-size: 14px;
}

.message-content.thinking::before {
    --bubble-color: #f8f9fa;
}

.message-content.assistant::before {
    --bubble-color: #ffffff;
}

.message-content.thinking.error {
    color: #dc2626;  /* 错误信息使用红色 */
}

.chat-input-container {
    padding: 24px 15%;
    background: #ffffff;
    border: none;
    position: relative;  /* 添加相对定位 */
}

.input-wrapper {
    position: relative;
    border: none;
    border-radius: 14px;
    background: #f5f5f7;  /* 改为浅灰色背景 */
    transition: all 0.3s ease;
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
    background: transparent;  /* 改为透明背景 */
    transition: all 0.3s ease;
}

textarea:focus {
    outline: none;
}

textarea::placeholder {
    color: #86868b;
}

.input-actions {
    position: absolute;
    right: 16px;  /* 调整按钮位置 */
    bottom: 16px;  /* 改为底部对齐 */
    transform: none;  /* 移除垂直居中 */
    display: flex;
    align-items: center;
}

.send-button {
    padding: 10px;  /* 增大按钮尺寸 */
    background: #0051a2;
    border: none;
    border-radius: 10px;
    color: #ffffff;
    cursor: pointer;
    transition: all 0.2s ease;
    margin-right: 4px;
}

.send-button:hover {
    background: #003d7a;  /* 加深发送按钮悬停颜色 */
    transform: scale(1.05);
}

.send-button:disabled {
    background: #cccccc;  /* 加深禁用状态颜色 */
    color: #666666;
    cursor: not-allowed;
    transform: none;
}

.send-button svg {
    width: 18px;  /* 增大图标尺寸 */
    height: 18px;
}

::-webkit-scrollbar {
    width: 6px;
}

::-webkit-scrollbar-track {
    background: transparent;
}

::-webkit-scrollbar-thumb {
    background: #b0b0b0;  /* 加深滚动条颜色 */
    border-radius: 3px;
}

:deep(*) {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
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
    border-bottom: 1px solid rgba(0, 81, 162, 0.2);
    transition: all 0.2s ease;
}

.message-content.assistant :deep(a:hover) {
    color: #003d7a;
    border-bottom-color: #003d7a;
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

/* 消息通用样式 */
.message {
    max-width: 80%;
    margin: 0 auto;
    animation: fadeIn 0.3s ease-in-out;
}

.message-wrapper {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

/* 思考消息样式 */
.message.thinking {
    background-color: #ffffff;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    padding: 20px;
    margin-bottom: 12px;
    border-left: 4px solid #1890ff;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    transform-origin: top;
    animation: thinkingSlideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.message.thinking:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
}

.message.thinking .message-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.message.thinking .avatar {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
}

.message.thinking .avatar:hover {
    transform: scale(1.05);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.message.thinking .avatar img {
    width: 22px;
    height: 22px;
    transition: all 0.3s ease;
}

.message.thinking .message-status {
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s ease;
}

.message.thinking .thinking-in-progress {
    color: #1890ff;
    display: flex;
    align-items: center;
    gap: 8px;
    animation: pulse 2s infinite;
}

.message.thinking .completed-thinking {
    color: #52c41a;
    display: flex;
    align-items: center;
    gap: 8px;
    animation: fadeIn 0.5s ease-out;
}

.message.thinking .message-content {
    font-size: 14px;
    line-height: 1.7;
    color: #333;
    padding: 16px;
    background: linear-gradient(to bottom, #f9f9f9, #f5f5f5);
    border-radius: 12px;
    white-space: pre-wrap;
    transition: all 0.3s ease;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.message.thinking .message-content:hover {
    background: linear-gradient(to bottom, #f5f5f5, #f0f0f0);
    box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.08);
}

/* 思考动画 */
.thinking-dots {
    display: flex;
    gap: 6px;
    align-items: center;
    padding: 4px;
}

.thinking-dots span {
    width: 8px;
    height: 8px;
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.thinking-dots span:nth-child(1) {
    animation-delay: -0.32s;
}

.thinking-dots span:nth-child(2) {
    animation-delay: -0.16s;
}

/* 动画效果 */
@keyframes thinkingSlideIn {
    from {
        opacity: 0;
        transform: translateY(20px) scale(0.98);
    }
    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

@keyframes pulse {
    0% {
        opacity: 1;
    }
    50% {
        opacity: 0.6;
    }
    100% {
        opacity: 1;
    }
}

@keyframes bounce {
    0%, 80%, 100% {
        transform: scale(0);
        opacity: 0.5;
    }
    40% {
        transform: scale(1);
        opacity: 1;
    }
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(5px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
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