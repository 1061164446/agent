import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { ChatMessage } from '../types/chat';

interface Chat {
    id: string;
    title: string;
    messages: ChatMessage[];
    updatedAt: Date;
}

export const useChatStore = defineStore('chat', () => {
    const chats = ref<Chat[]>([]);
    const currentChatId = ref<string>('');
    const loading = ref(false);

    const currentChat = computed(() => {
        return chats.value.find(chat => chat.id === currentChatId.value);
    });

    const sortedChats = computed(() => {
        return [...chats.value].sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime());
    });

    const createChat = () => {
        const newChat: Chat = {
            id: Date.now().toString(),
            title: '新对话',
            messages: [],
            updatedAt: new Date()
        };
        chats.value.push(newChat);
        currentChatId.value = newChat.id;
    };

    const setCurrentChat = (chatId: string) => {
        currentChatId.value = chatId;
    };

    const deleteChat = (chatId: string) => {
        const index = chats.value.findIndex(chat => chat.id === chatId);
        if (index !== -1) {
            chats.value.splice(index, 1);
            if (currentChatId.value === chatId) {
                currentChatId.value = chats.value[0]?.id || '';
            }
        }
    };

    const addMessage = (chatId: string, message: Omit<ChatMessage, 'id' | 'timestamp'>) => {
        const chat = chats.value.find(chat => chat.id === chatId);
        if (chat) {
            chat.messages.push({
                ...message,
                id: Date.now().toString(),
                timestamp: new Date()
            });
            chat.updatedAt = new Date();
        }
    };

    const sendMessage = async (content: string) => {
        if (!content.trim()) return;

        const userMessage: ChatMessage = {
            id: Date.now().toString(),
            role: 'user',
            content,
            timestamp: new Date()
        };

        addMessage(currentChatId.value, userMessage);
        loading.value = true;

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    content,
                    chatId: currentChatId.value
                })
            });

            if (!response.ok) {
                throw new Error('发送消息失败');
            }

            const data = await response.json();
            const assistantMessage: ChatMessage = {
                id: Date.now().toString(),
                role: 'assistant',
                content: data.content,
                timestamp: new Date()
            };

            addMessage(currentChatId.value, assistantMessage);
        } catch (error) {
            console.error('发送消息失败:', error);
        } finally {
            loading.value = false;
        }
    };

    return {
        chats,
        currentChatId,
        currentChat,
        sortedChats,
        loading,
        createChat,
        setCurrentChat,
        deleteChat,
        addMessage,
        sendMessage
    };
}); 