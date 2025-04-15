import axios from 'axios';
import type { ChatMessage } from '../types/chat';

const api = axios.create({
    baseURL: '/api/v1',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
});

/**
 * 发送聊天消息
 * 
 * @param sessionId 会话ID
 * @param message 消息内容
 * @returns 响应消息
 */
export async function sendMessage(sessionId: string, message: string): Promise<ChatMessage> {
    const response = await api.post<ChatMessage>('/chat', {
        sessionId,
        message
    });
    return response.data;
}

/**
 * 发送流式聊天消息
 * 
 * @param sessionId 会话ID
 * @param message 消息内容
 * @param onMessage 消息回调
 */
export function sendStreamMessage(
    sessionId: string,
    message: string,
    onMessage: (message: string) => void
): void {
    const eventSource = new EventSource(`/api/v1/stream/chat?sessionId=${sessionId}&message=${encodeURIComponent(message)}`);
    
    eventSource.onmessage = (event) => {
        onMessage(event.data);
    };
    
    eventSource.onerror = () => {
        eventSource.close();
    };
}

export const chat = async (message: string): Promise<string> => {
    const response = await axios.post('/api/chat/send', { message });
    return response.data.response;
}; 