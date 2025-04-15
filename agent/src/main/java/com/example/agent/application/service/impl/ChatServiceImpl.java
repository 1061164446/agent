package com.example.agent.application.service.impl;

import com.alibaba.cloud.ai.tongyi.chat.TongYiChatModel;
import com.alibaba.cloud.ai.tongyi.chat.TongYiChatOptions;

import com.alibaba.cloud.ai.tongyi.image.TongYiImagesModel;
import com.alibaba.cloud.ai.tongyi.image.TongYiImagesOptions;
import com.example.agent.application.service.ChatService;
import com.example.agent.domain.chat.ChatAggregate;
import com.example.agent.domain.chat.ChatValidator;
import com.example.agent.domain.exception.BusinessException;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务实现类
 * 提供文本对话、图片生成、语音识别、函数调用等功能
 * 
 * @author example
 * @version 1.0
 * @since 2024-04-14
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final TongYiChatModel tongYiChatModel;
    private final TongYiImagesModel tongYiImageModel;
    private final ChatValidator chatValidator;

    /**
     * 构造函数
     * @param tongYiChatModel 通义千问模型
     * @param tongYiImageModel 通义千问图片模型
     * @param chatValidator 聊天验证器
     */
    public ChatServiceImpl(TongYiChatModel tongYiChatModel, TongYiImagesModel tongYiImageModel, ChatValidator chatValidator) {
        this.tongYiChatModel = tongYiChatModel;
        this.tongYiImageModel = tongYiImageModel;
        this.chatValidator = chatValidator;
    }

    /**
     * 处理聊天请求
     * @param chatAggregate 聊天聚合对象
     * @return 处理后的聊天聚合对象
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @Override
    public ChatAggregate chat(ChatAggregate chatAggregate) {
        try {
            // 验证聊天请求
            chatValidator.validateChatRequest(chatAggregate);

            // 根据功能类型选择不同的处理方式
            if ("function".equals(chatAggregate.getFunctionType())) {
                return callFunction(chatAggregate);
            }

            // 调用通义千问API
            TongYiChatOptions options = TongYiChatOptions.builder()
                    .withModel("qwen-turbo")
                    .withTemperature(0.7d)
                    .withTopP(1.0d)
                    .build();

            // 创建消息数组
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的客服助手，请用专业、友好的方式回答用户的问题。"));

            // 如果有上下文消息，添加到消息列表
            if (chatAggregate.getContextMessages() != null) {
                for (String contextMessage : chatAggregate.getContextMessages()) {
                    messages.add(new UserMessage(contextMessage));
                }
            }

            // 添加当前用户消息
            messages.add(new UserMessage(chatAggregate.getContent()));

            // 调用通义千问API
            String responseText = tongYiChatModel.call(messages.toArray(new Message[0]));
            
            // 设置响应并验证
            chatAggregate.setResponse(responseText);
            chatValidator.validateChatResponse(chatAggregate);

            return chatAggregate;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理聊天请求时发生错误", e);
        }
    }

    /**
     * 处理流式聊天请求
     * @param chatAggregate 聊天聚合对象
     * @return 流式响应
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @Override
    public Flux<String> streamMessage(ChatAggregate chatAggregate) {
        try {
            // 验证聊天请求
            chatValidator.validateChatRequest(chatAggregate);

            // 调用通义千问API的流式接口
            TongYiChatOptions options = TongYiChatOptions.builder()
                    .withModel("qwen-turbo")
                    .withTemperature(0.7d)
                    .withTopP(1.0d)
                    .withMaxTokens(2048)
                    .build();

            // 创建消息数组
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的客服助手，请用专业、友好的方式回答用户的问题。"));

            // 如果有上下文消息，添加到消息列表
            if (chatAggregate.getContextMessages() != null) {
                for (String contextMessage : chatAggregate.getContextMessages()) {
                    messages.add(new UserMessage(contextMessage));
                }
            }

            // 添加当前用户消息
            messages.add(new UserMessage(chatAggregate.getContent()));

            // 创建Prompt对象
            Prompt prompt = new Prompt(messages, options);

            // 返回流式响应
            return tongYiChatModel.stream(prompt)
                    .map(response -> {
                        if (response.getResult() != null && 
                            response.getResult().getOutput() != null && 
                            response.getResult().getOutput().getContent() != null) {
                            return response.getResult().getOutput().getContent();
                        }
                        return "";
                    })
                    .filter(content -> !content.isEmpty())
                    // 确保每个响应作为单独事件发送
                    .distinctUntilChanged()
                    // 添加延迟以确保前端能够正确处理
                    .delayElements(Duration.ofMillis(50));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("处理流式聊天请求时发生错误", e);
        }
    }

    /**
     * 生成图片
     * @param chatAggregate 聊天聚合对象
     * @return 处理后的聊天聚合对象
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    @Override
    public ChatAggregate generateImage(ChatAggregate chatAggregate) {
        try {
            // 验证图片参数
            if (chatAggregate.getImageParams() == null) {
                throw new BusinessException("图片参数不能为空");
            }

            // 构建提示词
            String prompt = buildImagePrompt(chatAggregate);
            
            // 调用通义千问图片生成API
            TongYiImagesOptions options = TongYiImagesOptions.builder()
                .withModel("wanx-v1")  // 使用通义千问的图片生成模型
                .withN(chatAggregate.getImageParams().getN())  // 设置图片数量
                .build();

            // 创建图片生成请求
            ImagePrompt imagePrompt = new ImagePrompt(prompt, options);

            // 生成图片
            ImageResponse response = tongYiImageModel.call(imagePrompt);
            String imageUrl = response.getResult().getOutput().getUrl();
            
            // 设置响应并验证
            chatAggregate.setResponse(imageUrl);
            chatValidator.validateChatResponse(chatAggregate);

            return chatAggregate;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("生成图片时发生错误", e);
        }
    }

    /**
     * 调用函数
     * @param chatAggregate 聊天聚合对象
     * @return 处理后的聊天聚合对象
     * @throws BusinessException 当业务逻辑验证失败时抛出
     */
    private ChatAggregate callFunction(ChatAggregate chatAggregate) {
        try {
            // 验证函数调用参数
            chatValidator.validateFunctionParams(chatAggregate.getFunctionParams());

            // 构建函数调用提示词
            String prompt = buildFunctionPrompt(chatAggregate);

            // 调用通义千问API
            TongYiChatOptions options = TongYiChatOptions.builder()
                    .withModel("qwen-turbo")
                    .withTemperature(0.7d)
                    .withTopP(1.0d)
                    .build();

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的函数调用助手，请根据用户需求调用相应的函数。"));
            messages.add(new UserMessage(prompt));

            String responseText = tongYiChatModel.call(messages.toArray(new Message[0]));

            // 设置响应并验证
            chatAggregate.setResponse(responseText);
            chatValidator.validateChatResponse(chatAggregate);

            return chatAggregate;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用函数时发生错误", e);
        }
    }

    /**
     * 构建图片生成提示词
     * @param chatAggregate 聊天聚合对象
     * @return 图片生成提示词
     */
    private String buildImagePrompt(ChatAggregate chatAggregate) {
        StringBuilder prompt = new StringBuilder();
        
        // 添加用户输入的主要描述
        prompt.append("请生成一张图片，具体要求如下：\n");
        prompt.append("1. 主要场景：").append(chatAggregate.getContent()).append("\n");
        
        // 添加图片参数
        ChatAggregate.ImageParams params = chatAggregate.getImageParams();
        if (params != null) {
            if (params.getSize() != null) {
                prompt.append("2. 图片尺寸：").append(params.getSize()).append("\n");
            }
            if (params.getQuality() != null) {
                prompt.append("3. 图片质量：").append(params.getQuality()).append("\n");
            }
            if (params.getN() != null) {
                prompt.append("4. 生成数量：").append(params.getN()).append("\n");
            }
        }
        
        // 添加风格要求
        prompt.append("5. 风格要求：\n");
        prompt.append("   - 高清画质\n");
        prompt.append("   - 细节丰富\n");
        prompt.append("   - 构图合理\n");
        prompt.append("   - 色彩协调\n");
        
        return prompt.toString();
    }

    /**
     * 构建函数调用提示词
     * @param chatAggregate 聊天聚合对象
     * @return 函数调用提示词
     */
    private String buildFunctionPrompt(ChatAggregate chatAggregate) {
        ChatAggregate.FunctionParams params = chatAggregate.getFunctionParams();
        StringBuilder prompt = new StringBuilder();
        prompt.append("请调用函数：").append(params.getName());
        
        if (params.getDescription() != null) {
            prompt.append("，函数描述：").append(params.getDescription());
        }
        
        if (params.getArguments() != null) {
            prompt.append("，函数参数：").append(params.getArguments());
        }
        
        return prompt.toString();
    }
} 