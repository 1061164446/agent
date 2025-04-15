<template>
    <div class="app-container">
        <!-- 左侧聊天记录列表 -->
        <div class="chat-list">
            <div class="chat-list-header">
                <h1>AI Hub</h1>
                <button class="new-chat-btn" @click="startNewChat">
                    <span>+ 新对话</span>
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
                    <div class="message-content" :class="{ 'user': message.role === 'user' }">
                        {{ message.content }}
                    </div>
                </div>
            </div>
            <div class="chat-input-container">
                <textarea
                    v-model="userInput"
                    @keydown.enter.prevent="sendMessage"
                    placeholder="输入消息..."
                    rows="1"
                    ref="inputArea"
                ></textarea>
                <button @click="sendMessage" :disabled="isProcessing">发送</button>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { ChatService } from '../services/chatService';
import type { Message } from '../types/chat';

// 更新SVG图标
const assistantAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cGF0aCBkPSJNMTIgMkM2LjQ4IDIgMiA2LjQ4IDIgMTJzNC40OCAxMCAxMCAxMCAxMC00LjQ4IDEwLTEwUzE3LjUyIDIgMTIgMnptMCA0YzIuODUgMCA1LjE1IDIuMyA1LjE1IDUuMTVzLTIuMyA1LjE1LTUuMTUgNS4xNVM2Ljg1IDE0LjcgNi44NSAxMS4xNSA5LjE1IDYgMTIgNnoiIGZpbGw9IiMwMDAiLz48L3N2Zz4=';
const thinkingAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cGF0aCBkPSJNMTIgMkM2LjQ4IDIgMiA2LjQ4IDIgMTJzNC40OCAxMCAxMCAxMCAxMC00LjQ4IDEwLTEwUzE3LjUyIDIgMTIgMnptMCA0YzIuODUgMCA1LjE1IDIuMyA1LjE1IDUuMTVzLTIuMyA1LjE1LTUuMTUgNS4xNVM2Ljg1IDE0LjcgNi44NSAxMS4xNSA5LjE1IDYgMTIgNnptMi0ySDEwdjJoNFY0eiIgZmlsbD0iIzAwMCIvPjwvc3ZnPg==';

const chatService = new ChatService();
const messages = ref<Message[]>([]);
const userInput = ref('');
const isProcessing = ref(false);
const messagesContainer = ref<HTMLElement | null>(null);
const inputArea = ref<HTMLTextAreaElement | null>(null);
const currentChatId = ref<string>('1');
const chatHistory = ref([{ id: '1', title: '对话 1' }]);

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
        
        await chatService.processStream(reader, (response) => {
            if (response.type === 'thinking') {
                const newContent = response.content.trim();
                
                // 如果内容与上一次相同，不重复显示
                if (newContent === lastContent) {
                    return;
                }
                
                // 如果是新的思考步骤（数字开头）或第一条思考消息
                if (/^\d+\./.test(newContent) || !lastContent) {
                    messages.value.push({
                        role: 'thinking' as const,
                        content: newContent,
                        type: 'thinking' as const,
                        timestamp: Date.now()
                    });
                } else {
                    // 更新最后一个思考消息
                    const lastMessage = messages.value[messages.value.length - 1];
                    if (lastMessage.role === 'thinking') {
                        lastMessage.content = newContent;
                    }
                }
                
                lastContent = newContent;
                
                // 如果思考过程结束，标记进入回答阶段
                if (newContent.includes('思考过程结束')) {
                    isThinkingPhase = false;
                    currentResponse = null;
                }
            } else if (response.type === 'response') {
                const newContent = response.content.trim();
                
                // 如果是第一次收到回复或没有当前回复
                if (!currentResponse) {
                    currentResponse = {
                        role: 'assistant' as const,
                        content: newContent,
                        type: 'text' as const,
                        timestamp: Date.now()
                    };
                    messages.value.push(currentResponse);
                } else {
                    // 更新当前回复内容
                    currentResponse.content = newContent;
                }
                
                lastContent = newContent;
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
    nextTick(() => {
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
    });
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

onMounted(() => {
    scrollToBottom();
});
</script>

<style scoped>
.app-container {
    display: flex;
    width: 100%;
    height: 100vh;
    background: #fff;
}

.chat-list {
    width: 260px;
    border-right: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;
    background: #fafafa;
}

.chat-list-header {
    padding: 16px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.chat-list-header h1 {
    margin: 0 0 16px;
    font-size: 20px;
    font-weight: 600;
}

.new-chat-btn {
    width: 100%;
    padding: 8px 12px;
    background: #000;
    border: none;
    border-radius: 6px;
    color: #fff;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    justify-content: center;
}

.chat-list-items {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
}

.chat-list-item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 6px;
    cursor: pointer;
    margin-bottom: 4px;
    color: rgba(0, 0, 0, 0.6);
    font-size: 14px;
}

.chat-list-item:hover {
    background: rgba(0, 0, 0, 0.05);
}

.chat-list-item.active {
    background: rgba(0, 0, 0, 0.08);
    color: #000;
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
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px 15% 0;
    display: flex;
    flex-direction: column;
}

.message {
    display: flex;
    margin-bottom: 20px;
}

.message.user {
    justify-content: flex-end;
}

.message-content {
    max-width: 80%;
    padding: 12px 16px;
    border-radius: 12px;
    font-size: 15px;
    line-height: 1.6;
    white-space: pre-wrap;
    background: #f3f3f3;
    border-bottom-left-radius: 2px;
}

.message-content.user {
    background: #000;
    color: #fff;
    border-bottom-right-radius: 2px;
    border-bottom-left-radius: 12px;
}

.message.thinking {
    margin-bottom: 4px;
}

.message.thinking .message-content {
    position: relative;
    padding: 8px 16px 8px 24px;
    background: transparent;
    color: rgba(0, 0, 0, 0.6);
    font-size: 14px;
    line-height: 1.6;
    border-radius: 0;
}

.message.thinking .message-content::before {
    content: '';
    position: absolute;
    left: 6px;
    top: 0;
    bottom: 0;
    width: 1px;
    background: rgba(0, 0, 0, 0.2);
}

.message.thinking .message-content::after {
    content: '';
    position: absolute;
    left: 4px;
    top: 12px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.3);
}

.chat-input-container {
    padding: 24px 15%;
    border-top: 1px solid rgba(0, 0, 0, 0.1);
    position: relative;
}

textarea {
    width: 100%;
    padding: 12px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    resize: none;
    font-size: 14px;
    line-height: 1.6;
    transition: border-color 0.2s;
}

textarea:focus {
    outline: none;
    border-color: #000;
}

button {
    position: absolute;
    right: calc(15% + 12px);
    bottom: 36px;
    padding: 4px 12px;
    background: transparent;
    color: rgba(0, 0, 0, 0.5);
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
}

button:hover:not(:disabled) {
    background: rgba(0, 0, 0, 0.05);
    color: #000;
}

button:disabled {
    color: rgba(0, 0, 0, 0.3);
    cursor: not-allowed;
}

.generated-image {
    max-width: 100%;
    border-radius: 12px;
    margin-top: 12px;
}

/* 添加滚动条样式 */
::-webkit-scrollbar {
    width: 4px;
}

::-webkit-scrollbar-track {
    background: transparent;
}

::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.2);
    border-radius: 4px;
}
</style> 