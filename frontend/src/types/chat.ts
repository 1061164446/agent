/**
 * 聊天消息类型
 */
export interface ChatMessage {
    id: string;
    role: 'user' | 'assistant';
    content: string;
    timestamp: Date;
}

/**
 * 聊天会话类型
 */
export interface ChatSession {
    id: string;
    title: string;
    lastMessage: string;
    lastTime: Date;
}

/**
 * 聊天状态类型
 */
export interface ChatState {
    sessions: ChatSession[];
    currentSession: ChatSession | null;
    messages: ChatMessage[];
    loading: boolean;
    streaming: boolean;
}

export interface Message {
    role: 'user' | 'assistant' | 'thinking';
    content: string;
    type: 'text' | 'image' | 'thinking';
    timestamp?: number;
}

export interface ChatRequest {
    content: string;
}

export interface ChatResponse {
    type: 'thinking' | 'response';
    content: string;
    timestamp: number;
} 