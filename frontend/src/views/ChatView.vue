<template>
    <div class="chat-container">
        <div class="chat-header">
            <div class="chat-type-switch">
                <button 
                    :class="['switch-button', chatType === 'text' ? 'active' : '']"
                    @click="switchChatType('text')"
                >
                    文本对话
                </button>
                <button 
                    :class="['switch-button', chatType === 'image' ? 'active' : '']"
                    @click="switchChatType('image')"
                >
                    图片生成
                </button>
            </div>
        </div>
        <div class="chat-messages" ref="messagesContainer">
            <div v-for="(message, index) in messages" :key="index" 
                 :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']">
                <div class="message-content">
                    <template v-if="message.type === 'image'">
                        <img :src="message.content" alt="生成的图片" class="generated-image" />
                    </template>
                    <template v-else>
                        {{ message.content }}
                    </template>
                </div>
            </div>
            <div v-if="isTyping" class="message assistant-message">
                <div class="message-content">
                    <template v-if="chatType === 'image'">
                        <div class="loading-image">
                            <div class="loading-spinner"></div>
                            <span>正在生成图片...</span>
                        </div>
                    </template>
                    <template v-else>
                        {{ currentMessage }}
                        <span class="typing-indicator">▋</span>
                    </template>
                </div>
            </div>
        </div>
        <div class="chat-input-container">
            <textarea
                v-model="userInput"
                @keydown.enter.prevent="sendMessage"
                :placeholder="chatType === 'image' ? '描述您想要的图片...' : '输入消息...'"
                rows="1"
                ref="inputArea"
            ></textarea>
            <button @click="sendMessage" :disabled="isTyping">
                <span class="button-text">{{ chatType === 'image' ? '生成' : '发送' }}</span>
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue';
import { ChatService } from '../services/chatService';

const chatService = new ChatService();
const messages = ref<Array<{ role: 'user' | 'assistant', content: string, type?: 'text' | 'image' }>>([]);
const userInput = ref('');
const isTyping = ref(false);
const currentMessage = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const inputArea = ref<HTMLTextAreaElement | null>(null);
const chatType = ref<'text' | 'image'>('text');

// 自动调整输入框高度
const adjustInputHeight = () => {
    if (inputArea.value) {
        inputArea.value.style.height = 'auto';
        inputArea.value.style.height = inputArea.value.scrollHeight + 'px';
    }
};

// 监听输入变化
watch(userInput, () => {
    nextTick(adjustInputHeight);
});

// 切换聊天类型
const switchChatType = (type: 'text' | 'image') => {
    chatType.value = type;
    messages.value = [];
    currentMessage.value = '';
    isTyping.value = false;
};

// 发送消息
const sendMessage = async () => {
    if (!userInput.value.trim() || isTyping.value) return;

    const userMessage = userInput.value.trim();
    console.log('发送用户消息:', userMessage);
    messages.value.push({ 
        role: 'user', 
        content: userMessage,
        type: 'text'  // 用户消息始终是文本类型
    });
    userInput.value = '';
    isTyping.value = true;
    currentMessage.value = '';

    try {
        if (chatType.value === 'text') {
            console.log('开始文本对话...');
            await chatService.streamMessage(
                userMessage,
                (text) => {
                    console.log('收到流式响应:', text);
                    setTimeout(() => {
                        currentMessage.value += text;
                        scrollToBottom();
                    }, 50);
                },
                (error) => {
                    console.error('流式响应错误:', error);
                    isTyping.value = false;
                    messages.value.push({ 
                        role: 'assistant', 
                        content: '抱歉，发生了错误，请稍后重试。',
                        type: 'text'
                    });
                },
                () => {
                    console.log('流式响应完成，当前消息:', currentMessage.value);
                    if (currentMessage.value) {
                        messages.value.push({ 
                            role: 'assistant', 
                            content: currentMessage.value,
                            type: 'text'
                        });
                        console.log('消息已添加到历史记录');
                    }
                    isTyping.value = false;
                    currentMessage.value = '';
                    scrollToBottom();
                }
            );
        } else {
            console.log('开始生成图片...');
            try {
                const imageUrl = await chatService.generateImage(userMessage);
                messages.value.push({ 
                    role: 'assistant', 
                    content: imageUrl,
                    type: 'image'
                });
            } catch (error) {
                console.error('图片生成错误:', error);
                messages.value.push({ 
                    role: 'assistant', 
                    content: `图片生成失败: ${error instanceof Error ? error.message : '未知错误'}`,
                    type: 'text'
                });
            } finally {
                isTyping.value = false;
                scrollToBottom();
            }
        }
    } catch (error) {
        console.error('发送消息错误:', error);
        isTyping.value = false;
        messages.value.push({ 
            role: 'assistant', 
            content: '抱歉，发生了错误，请稍后重试。',
            type: 'text'
        });
    }
};

// 滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        if (messagesContainer.value) {
            console.log('滚动到底部');
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
    });
};

onMounted(() => {
    scrollToBottom();
});
</script>

<style scoped>
.chat-container {
    max-width: 800px;
    margin: 0 auto;
    height: 100vh;
    display: flex;
    flex-direction: column;
    background-color: #f5f5f7;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    position: relative;
    overflow: hidden;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chat-header {
    padding: 20px;
    background-color: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    text-align: center;
    position: sticky;
    top: 0;
    z-index: 10;
    border-top-left-radius: 12px;
    border-top-right-radius: 12px;
}

.chat-header h1 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #1d1d1f;
    letter-spacing: -0.5px;
}

.chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    scroll-behavior: smooth;
    background-color: rgba(245, 245, 247, 0.5);
    border-left: 1px solid rgba(0, 0, 0, 0.05);
    border-right: 1px solid rgba(0, 0, 0, 0.05);
}

.message {
    max-width: 80%;
    padding: 12px 16px;
    border-radius: 18px;
    line-height: 1.4;
    font-size: 16px;
    animation: fadeIn 0.3s ease-out;
    position: relative;
    border: 1px solid rgba(0, 0, 0, 0.05);
    margin: 8px 0;
}

.user-message {
    align-self: flex-end;
    background-color: #007AFF;
    color: white;
    border-bottom-right-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 122, 255, 0.2);
    border: none;
    margin-left: auto;
}

.assistant-message {
    align-self: flex-start;
    background-color: white;
    color: #1d1d1f;
    border-bottom-left-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(0, 0, 0, 0.05);
    margin-right: auto;
}

.message-content {
    white-space: pre-wrap;
    word-break: break-word;
    line-height: 1.5;
}

.typing-indicator {
    display: inline-block;
    animation: blink 1s infinite;
    margin-left: 2px;
    color: #007AFF;
}

.chat-input-container {
    padding: 16px;
    background-color: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-top: 1px solid rgba(0, 0, 0, 0.1);
    display: flex;
    gap: 12px;
    align-items: flex-end;
    position: sticky;
    bottom: 0;
    z-index: 10;
    border-bottom-left-radius: 12px;
    border-bottom-right-radius: 12px;
}

textarea {
    flex: 1;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 18px;
    padding: 12px 16px;
    font-size: 16px;
    line-height: 1.4;
    background-color: rgba(245, 245, 247, 0.8);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    resize: none;
    outline: none;
    max-height: 120px;
    transition: all 0.2s ease;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

textarea:focus {
    background-color: rgba(229, 229, 231, 0.8);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    border-color: rgba(0, 122, 255, 0.3);
}

button {
    background-color: #007AFF;
    color: white;
    border: none;
    border-radius: 18px;
    padding: 12px 24px;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 2px 4px rgba(0, 122, 255, 0.2);
}

button:disabled {
    background-color: #8e8e93;
    cursor: not-allowed;
    box-shadow: none;
}

button:hover:not(:disabled) {
    background-color: #0062cc;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 122, 255, 0.3);
}

button:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: 0 2px 4px rgba(0, 122, 255, 0.2);
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

@keyframes blink {
    0%, 100% {
        opacity: 1;
    }
    50% {
        opacity: 0;
    }
}

/* 自定义滚动条样式 */
.chat-messages::-webkit-scrollbar {
    width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
    background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.2);
    border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
    background-color: rgba(0, 0, 0, 0.3);
}

.chat-type-switch {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-bottom: 16px;
}

.switch-button {
    background-color: transparent;
    color: #1d1d1f;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 20px;
    padding: 8px 16px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
}

.switch-button.active {
    background-color: #007AFF;
    color: white;
    border-color: #007AFF;
}

.generated-image {
    max-width: 100%;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.loading-image {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 16px;
}

.loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid rgba(0, 122, 255, 0.1);
    border-top-color: #007AFF;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}
</style> 