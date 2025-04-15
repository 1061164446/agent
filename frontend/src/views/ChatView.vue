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
                                思考中...
                            </div>
                        </div>
                        <div class="message-content" :class="message.role">
                            {{ message.content }}
                        </div>
                    </div>
                </div>
            </div>
            <div class="chat-input-container">
                <div class="input-wrapper">
                    <textarea
                        v-model="userInput"
                        @keydown="handleKeyDown"
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

const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
        if (!e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    }
};

const sendMessage = async () => {
    if (!userInput.value.trim() || isProcessing.value) return;

    const userMessage = userInput.value.trim();
    messages.value.push({
        role: 'user',
        content: userMessage,
        type: 'text',
        timestamp: Date.now()
    });

    userInput.value = '';
    isProcessing.value = true;

    try {
        const reader = await chatService.streamMessageWithThinking(userMessage);
        let lastContent = '';
        let isThinkingPhase = true;
        let currentResponse: Message | null = null;
        let accumulatedContent = '';
        
        await chatService.processStream(reader, async (response) => {
            if (response.type === 'thinking') {
                const newContent = response.content.trim();
                
                if (newContent === lastContent) {
                    return;
                }

                if (/^\d+\./.test(newContent) || !lastContent) {
                    messages.value.push({
                        role: 'thinking' as const,
                        content: newContent,
                        type: 'thinking' as const,
                        timestamp: Date.now()
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
                }
            } else if (response.type === 'response') {
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
                    // 每次增加一个字符
                    const diff = newContent.slice(accumulatedContent.length);
                    if (diff) {
                        accumulatedContent = newContent;
                        await new Promise(resolve => setTimeout(resolve, 20)); // 添加小延迟
                        currentResponse.content = accumulatedContent;
                    }
                }
            }
            scrollToBottom();
        });
    } catch (error) {
        console.error('Error:', error);
        messages.value.push({
            role: 'assistant',
            content: '抱歉，发生了错误，请稍后重试。',
            type: 'text',
            timestamp: Date.now()
        });
    } finally {
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
    padding-left: 44px;  /* 添加左边距，与思考内容对齐 */
    line-height: 1.6;
    font-size: 15px;
    white-space: pre-wrap;
    margin-top: 0;
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
</style> 