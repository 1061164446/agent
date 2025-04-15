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
                        <div class="message-content" :class="message.role" v-if="message.role === 'assistant'" v-html="formatMarkdown(message.content)"></div>
                        <div class="message-content" :class="message.role" v-else>{{ message.content }}</div>
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
import { marked } from 'marked';

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
                
                if (newContent.includes('思考过程结束')) {
                    isThinkingPhase = false;
                    currentResponse = null;
                    accumulatedContent = '';
                    
                    // 清除思考计时器
                    if (thinkingTimerId !== null) {
                        clearTimeout(thinkingTimerId);
                        thinkingTimerId = null;
                    }
                    
                    // 添加调试日志
                    console.log('思考过程结束，更新思考消息状态');
                    
                    // 只更新现有思考消息的状态，不添加新消息
                    messages.value.forEach(msg => {
                        if (msg.role === 'thinking') {
                            msg.thinkingCompleted = true;
                            msg.thinkingTime = Math.floor((Date.now() - thinkingStartTime) / 1000);
                        }
                    });
                    
                    // 如果有多条包含"思考过程结束"的消息，只保留一条
                    const endMessages = messages.value.filter(
                        msg => msg.role === 'thinking' && msg.content.includes('思考过程结束')
                    );
                    if (endMessages.length > 1) {
                        for (let i = 1; i < endMessages.length; i++) {
                            const index = messages.value.indexOf(endMessages[i]);
                            if (index !== -1) {
                                messages.value.splice(index, 1);
                            }
                        }
                    }
                }
            } else if (response.type === 'response') {
                const newContent = response.content.trim();
                
                if (!currentResponse) {
                    // 检查是否已经有相似内容的响应消息
                    const hasSimilarResponse = messages.value.some(
                        msg => msg.role === 'assistant' && 
                               msg.content && 
                               msg.content.length > 0 &&
                               Date.now() - (msg.timestamp || 0) < 5000
                    );
                    
                    if (!hasSimilarResponse) {
                        currentResponse = {
                            role: 'assistant' as const,
                            content: newContent,
                            type: 'text' as const,
                            timestamp: Date.now()
                        };
                        messages.value.push(currentResponse);
                        accumulatedContent = newContent;
                    }
                } else {
                    // 更新现有响应消息
                    if (newContent !== accumulatedContent) {
                        const addedContent = newContent.slice(accumulatedContent.length);
                        if (addedContent) {
                            // 确保内容正确追加
                            currentResponse.content = newContent;
                            accumulatedContent = newContent;
                            
                            // 强制更新视图
                            await nextTick();
                            messages.value = [...messages.value];
                        }
                    }
                }
            }
            
            // 每次接收到新内容都滚动到底部
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
        // 清除思考计时器
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
    background: #ebebeb;  /* 加深左侧背景色 */
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

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px 0;
    background: #ffffff;
}

.message {
    padding: 0 15%;
    margin-bottom: 24px;
    position: relative;  /* 添加相对定位 */
}

.message.thinking::before {
    content: '';
    position: absolute;
    left: calc(15% + 34px);  /* 向左调整，与思考文字保持一定距离 */
    top: 60px;  /* 进一步下移，避开"思考过程开始"文字 */
    height: calc(100% - 65px);  /* 调整高度 */
    width: 1px;
    background: #e5e7eb;
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
    margin-left: auto;
}

.message-header {
    display: flex;
    align-items: center;
    margin-bottom: 12px;  /* 减少头部与内容的间距 */
}

.message-header.assistant-header {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
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

.avatar img {
    width: 24px;
    height: 24px;
    object-fit: contain;
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
    font-size: 14px;
    line-height: 1.6;
    color: #111827;
    white-space: pre-wrap;
    max-width: 80%;
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
    color: #000000;
    padding-left: 44px;
    line-height: 1.6;
    font-size: 15px;
    white-space: pre-wrap;
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
    color: #666666;
    padding-left: 44px;
    font-size: 14px;
    line-height: 1.8;
    margin-top: 4px;  /* 添加一点间距 */
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

/* 添加打字机效果相关的CSS */
.message-content.assistant.typewriter-effect {
    width: fit-content;
    position: relative;
}

.message-content.assistant.typewriter-effect::before {
    content: attr(data-content);
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    white-space: pre-wrap;
    overflow: hidden;
    color: #000000;
    border-right: 3px solid transparent;
}

/* 解决打字机效果的CSS隔离问题 */
:deep(.typewriter-effect) {
    width: fit-content;
    position: relative;
}

:deep(.typewriter-effect::before) {
    content: "";
    display: block;
    position: absolute;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent calc(var(--visible-length, 0) * 1ch), #fff calc(var(--visible-length, 0) * 1ch + 0.1ch));
    pointer-events: none;
}
</style> 