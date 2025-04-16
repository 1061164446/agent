import axios from 'axios';
import { SSE } from 'sse.js';
import { ChatRequest, ChatResponse } from '../types/chat';

/**
 * 聊天服务类
 * 提供与后端API的通信功能，支持：
 * 1. 文本对话
 * 2. 流式对话
 * 3. 多轮对话
 * 4. 函数调用
 * 5. 图片生成
 * 6. 语音识别
 */
export class ChatService {
    private baseUrl: string;

    constructor() {
        this.baseUrl = 'http://localhost:8080';  // 移除 /api 前缀，因为代理已经处理了
    }

    /**
     * 发送单轮文本对话
     * @param message 用户消息
     * @returns 聊天响应
     */
    async sendMessage(message: string): Promise<any> {
        try {
            const response = await axios.post(`${this.baseUrl}/api/chat/send`, {
                message: message
            });
            return response.data;
        } catch (error) {
            console.error('发送消息失败:', error);
            throw error;
        }
    }

    /**
     * 发送流式文本对话
     * @param message 用户消息
     * @param onMessage 消息回调函数
     * @param onError 错误回调函数
     * @param onComplete 完成回调函数
     */
    streamMessage(
        message: string,
        onMessage: (text: string) => void,
        onError: (error: Error) => void,
        onComplete: () => void
    ): void {
        const chatAggregate = {
            message: message,
            functionType: 'chat'
        };

        console.log('开始SSE连接');
        const eventSource = new SSE(`${this.baseUrl}/api/chat/stream`, {
            headers: {
                'Content-Type': 'application/json'
            },
            payload: JSON.stringify(chatAggregate)
        });

        let accumulatedMessage = '';
        let isCompleted = false;
        let lastMessageTime = Date.now();

        const closeConnection = () => {
            if (!isCompleted) {
                isCompleted = true;
                console.log('关闭SSE连接，当前状态:', eventSource.readyState);
                eventSource.close();
                onComplete();
            }
        };

        eventSource.onmessage = (event) => {
            try {
                const content = event.data;
                console.log('收到消息:', content);
                if (content) {
                    // 更新最后消息时间
                    lastMessageTime = Date.now();
                    
                    // 检查是否是结束标记
                    if (content === '[DONE]') {
                        console.log('收到结束标记');
                        closeConnection();
                        return;
                    }

                    // 只处理新的内容
                    if (content.length > accumulatedMessage.length) {
                        const newContent = content.substring(accumulatedMessage.length);
                        accumulatedMessage = content;
                        onMessage(newContent);
                    }
                }
            } catch (error) {
                console.error('处理消息错误:', error);
                onError(error instanceof Error ? error : new Error('Unknown error'));
                closeConnection();
            }
        };

        eventSource.onerror = (error) => {
            console.error('SSE错误:', error);
            onError(error instanceof Error ? error : new Error('Unknown error'));
            closeConnection();
        };

        // 监听完成事件
        eventSource.addEventListener('done', () => {
            console.log('SSE连接完成，当前状态:', eventSource.readyState);
            closeConnection();
        });

        // 监听连接关闭事件
        eventSource.addEventListener('close', () => {
            console.log('SSE连接已关闭，当前状态:', eventSource.readyState);
            closeConnection();
        });

        // 检查消息间隔，如果超过5秒没有新消息，认为消息已结束
        const checkMessageInterval = setInterval(() => {
            if (!isCompleted && Date.now() - lastMessageTime > 5000) {
                console.log('消息间隔超时，自动关闭');
                clearInterval(checkMessageInterval);
                closeConnection();
            }
        }, 1000);

        // 设置超时自动关闭
        setTimeout(() => {
            if (!isCompleted) {
                console.log('SSE连接超时，自动关闭，当前状态:', eventSource.readyState);
                clearInterval(checkMessageInterval);
                closeConnection();
            }
        }, 30000); // 30秒超时
    }

    /**
     * 发送多轮对话
     * @param message 用户消息
     * @param contextMessages 上下文消息数组
     * @returns 聊天响应
     */
    async sendConversation(message: string, contextMessages: string[]): Promise<any> {
        try {
            const response = await axios.post(`${this.baseUrl}/api/chat/conversation`, {
                message: message,
                contextMessages: contextMessages
            });
            return response.data;
        } catch (error) {
            console.error('发送多轮对话失败:', error);
            throw error;
        }
    }

    /**
     * 调用函数
     * @param functionName 函数名称
     * @param functionDescription 函数描述
     * @param functionArguments 函数参数
     * @returns 函数执行结果
     */
    async callFunction(
        functionName: string,
        functionDescription: string,
        functionArguments: any
    ): Promise<any> {
        try {
            const response = await axios.post(`${this.baseUrl}/api/chat/function`, {
                functionName: functionName,
                functionDescription: functionDescription,
                functionArguments: functionArguments
            });
            return response.data;
        } catch (error) {
            console.error('调用函数失败:', error);
            throw error;
        }
    }

    /**
     * 生成图片
     * @param prompt 图片描述
     * @returns 图片URL
     */
    async generateImage(prompt: string): Promise<string> {
        try {
            if (!prompt || prompt.trim() === '') {
                throw new Error('图片描述不能为空');
            }

            console.log('开始生成图片，提示词:', prompt);
            const chatAggregate = {
                message: prompt,
                functionType: 'image',
                imageParams: {
                    size: '1024x1024',  // 图片大小：256x256, 512x512, 1024x1024
                    n: 1,  // 图片数量：1-4
                    quality: 'standard'  // 图片质量：standard, hd
                },
                response: '',  // AI响应
                contextMessages: [],  // 上下文消息列表
                sessionId: '',  // 会话ID
                speechParams: null,  // 语音识别参数
                functionParams: null  // 函数调用参数
            };

            const response = await fetch(`${this.baseUrl}/api/chat/image`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify(chatAggregate)
            });

            console.log('图片生成响应状态:', response.status);
            
            if (!response.ok) {
                const errorText = await response.text();
                console.error('图片生成失败，错误信息:', errorText);
                throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
            }

            const data = await response.json();
            console.log('图片生成成功，返回数据:', data);
            
            if (!data.response) {
                throw new Error('返回数据中没有图片URL');
            }

            return data.response;
        } catch (error) {
            console.error('生成图片错误:', error);
            throw new Error(`生成图片失败: ${error instanceof Error ? error.message : '未知错误'}`);
        }
    }

    /**
     * 语音识别
     * @param audioData 音频数据
     * @returns 识别结果
     */
    async recognizeSpeech(audioData: Blob): Promise<any> {
        try {
            const formData = new FormData();
            formData.append('audio', audioData);
            const response = await axios.post(`${this.baseUrl}/api/chat/speech`, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            });
            return response.data;
        } catch (error) {
            console.error('语音识别失败:', error);
            throw error;
        }
    }

    /**
     * 发送流式文本对话，包含思考过程
     * @param content 用户消息
     * @returns 响应流读取器
     */
    async streamMessageWithThinking(content: string): Promise<ReadableStreamDefaultReader<Uint8Array>> {
        console.log('开始streamMessageWithThinking请求');
        try {
            const response = await fetch(`${this.baseUrl}/api/chat/send/thinking`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ content } as ChatRequest),
                cache: 'no-store'
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error('流式请求失败:', response.status, response.statusText, errorText);
                throw new Error(`Stream request failed: ${response.status} ${response.statusText} - ${errorText}`);
            }

            if (!response.body) {
                console.error('响应没有body');
                throw new Error('No response body');
            }

            console.log('成功获取流响应');
            return response.body.getReader();
        } catch (error) {
            console.error('streamMessageWithThinking 错误:', error);
            throw error;
        }
    }

    /**
     * 处理流式响应数据
     * @param reader 响应流读取器
     * @param onMessage 消息处理回调
     */
    async processStream(
        reader: ReadableStreamDefaultReader<Uint8Array>,
        onMessage: (response: ChatResponse) => void
    ): Promise<void> {
        const decoder = new TextDecoder();
        let buffer = '';

        try {
            console.log('开始处理流数据');
            while (true) {
                const { done, value } = await reader.read();
                
                if (done) {
                    console.log('流读取完成');
                    // 处理剩余缓冲区
                    if (buffer.trim()) {
                        try {
                            const response = JSON.parse(buffer.trim()) as ChatResponse;
                            onMessage(response);
                        } catch (e) {
                            console.error('解析最后缓冲区JSON失败:', e, 'buffer:', buffer);
                        }
                    }
                    break;
                }

                const chunk = decoder.decode(value, { stream: true });
                buffer += chunk;
                
                // 处理缓冲区中的完整JSON对象
                const lines = buffer.split('\n');
                // 保留最后一个可能不完整的行
                buffer = lines.pop() || '';

                // 收集所有行的内容
                let fullContent = '';
                for (const line of lines) {
                    if (line.trim()) {
                        try {
                            const response = JSON.parse(line) as ChatResponse;
                            if (response.type === 'response') {
                                fullContent += response.content;
                            } else {
                                onMessage(response);
                            }
                        } catch (e) {
                            console.error('解析JSON失败:', e, 'line:', line);
                        }
                    }
                }

                // 如果有完整的响应内容，一次性发送
                if (fullContent) {
                    onMessage({
                        type: 'response',
                        content: fullContent,
                        timestamp: Date.now()
                    });
                }
            }
        } catch (error) {
            console.error('流处理错误:', error);
            throw error;
        } finally {
            console.log('释放流读取器');
            reader.releaseLock();
        }
    }
} 